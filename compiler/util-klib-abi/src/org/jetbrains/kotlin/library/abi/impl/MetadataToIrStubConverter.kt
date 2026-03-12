/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi.impl

import kotlinx.metadata.klib.KlibModuleMetadata
import kotlinx.metadata.klib.fqName
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrAnnotationImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NativeStandardInteropNames
import org.jetbrains.kotlin.types.Variance
import java.util.*
import kotlin.metadata.*
import kotlin.metadata.ClassKind as KmClassKind
import kotlin.metadata.Visibility as KmVisibility
import org.jetbrains.kotlin.descriptors.ClassKind as IrClassKind

/**
 * Converts [KlibModuleMetadata] (Km* types) into lightweight IR declaration stubs
 * for use by [CInteropIdSignatureComputer] to compute IdSignatures.
 *
 * The stubs carry enough information (visibility, modality, ObjC annotations, etc.)
 * for both signature computation and ABI reading.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class, ExperimentalContextParameters::class, ExperimentalAnnotationsInMetadata::class)
internal class MetadataToIrStubConverter(private val module: KlibModuleMetadata) {
    private val irFactory = IrFactory(StageController())

    // Package fragments
    private val packageFragments = HashMap<String, IrExternalPackageFragment>()

    // Class symbols (ClassName → IrClassSymbol)
    private val classSymbols = HashMap<String, IrClassSymbol>()

    // Class type parameter scopes (ClassName → TypeParameterScope)
    private val classScopes = HashMap<String, TypeParameterScope>()

    // ---- Lookup maps ----

    /** ClassName -> IrClass (public: used by [MetadataLibraryAbiReaderImpl] to find top-level classes) */
    val classStubs = HashMap<String, IrClass>()

    private val functionStubs = IdentityHashMap<KmFunction, IrSimpleFunction>()
    private val constructorStubs = IdentityHashMap<KmConstructor, IrConstructor>()
    private val propertyStubs = IdentityHashMap<KmProperty, IrProperty>()
    private val enumEntryStubs = IdentityHashMap<KmEnumEntry, IrEnumEntry>()

    /** IrDeclaration -> list of Km annotations (side-map to avoid creating fake IrConstructorCall objects) */
    val annotationsMap = IdentityHashMap<IrDeclaration, List<KmAnnotation>>()

    // ---- ObjC annotation stub infrastructure ----

    /** Lazily created annotation constructor symbols for ObjC interop annotations. */
    private val objCAnnotationConstructors = HashMap<String, IrConstructorSymbol>()

    /**
     * Creates a minimal annotation class + constructor stub for the given [classId].
     * The constructor has value parameters matching [parameterNames] and [parameterTypes].
     */
    private fun getOrCreateAnnotationConstructor(
        classId: ClassId,
        parameterNames: List<String>,
        parameterTypes: List<() -> IrType>,
    ): IrConstructorSymbol {
        val className = classId.asClassName()
        objCAnnotationConstructors[className]?.let { return it }

        val classSymbol = getOrCreateClassSymbol(className)
        val irClass = classSymbol.owner

        val ctorSymbol = IrConstructorSymbolImpl()
        val irCtor = irFactory.createConstructor(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
            name = Name.special("<init>"),
            visibility = DescriptorVisibilities.PUBLIC,
            isInline = false,
            isExpect = false,
            returnType = IrSimpleTypeImpl(classSymbol, SimpleTypeNullability.NOT_SPECIFIED, emptyList(), emptyList()),
            symbol = ctorSymbol,
            isPrimary = true,
        )
        irCtor.parent = irClass
        irCtor.parameters = parameterNames.zip(parameterTypes).map { (name, typeFn) ->
            createValueParameter(name, typeFn(), IrParameterKind.Regular)
        }
        irClass.declarations.add(irCtor)

        objCAnnotationConstructors[className] = ctorSymbol
        return ctorSymbol
    }

    private fun ClassId.asClassName(): String {
        val packagePart = packageFqName.asString().replace('.', '/')
        val classPart = relativeClassName.asString()
        return if (packagePart.isEmpty()) classPart else "$packagePart/$classPart"
    }

    private fun stringType(): IrType = IrSimpleTypeImpl(
        getOrCreateClassSymbol("kotlin/String"),
        SimpleTypeNullability.NOT_SPECIFIED, emptyList(), emptyList()
    )

    private fun booleanType(): IrType = IrSimpleTypeImpl(
        getOrCreateClassSymbol("kotlin/Boolean"),
        SimpleTypeNullability.NOT_SPECIFIED, emptyList(), emptyList()
    )

    /**
     * Converts ObjC-related [KmAnnotation]s to real [IrAnnotation]s and attaches them
     * to the given [irDeclaration]. Non-ObjC annotations are ignored (they remain in [annotationsMap]).
     */
    private fun attachObjCAnnotations(irDeclaration: IrMutableAnnotationContainer, kmAnnotations: List<KmAnnotation>) {
        if (kmAnnotations.isEmpty()) return
        for (ann in kmAnnotations) {
            val irAnn = convertObjCAnnotation(ann) ?: continue
            irDeclaration.annotations = irDeclaration.annotations + irAnn
        }
    }

    private fun convertObjCAnnotation(ann: KmAnnotation): IrAnnotation? {
        val selectorEncodingStretArgs = listOf(
            Triple("selector", ::stringType, ::stringConst),
            Triple("encoding", ::stringType, ::stringConst),
            Triple("isStret", ::booleanType, ::booleanConst),
        )
        val (classId, argSpecs) = when (ann.className) {
            OBJC_METHOD_CLASS_NAME -> NativeStandardInteropNames.objCMethodClassId to selectorEncodingStretArgs
            OBJC_FACTORY_CLASS_NAME -> NativeStandardInteropNames.objCFactoryClassId to selectorEncodingStretArgs
            OBJC_CONSTRUCTOR_CLASS_NAME -> NativeStandardInteropNames.objCConstructorClassId to listOf(
                Triple("initSelector", ::stringType, ::stringConst),
                Triple("designated", ::booleanType, ::booleanConst),
            )
            OBJC_DIRECT_CLASS_NAME -> NativeStandardInteropNames.objCDirectClassId to listOf(
                Triple("symbol", ::stringType, ::stringConst),
            )
            else -> return null
        }
        val ctorSymbol = getOrCreateAnnotationConstructor(classId, argSpecs.map { it.first }, argSpecs.map { it.second })
        return createAnnotation(ctorSymbol, ann, argSpecs.map { it.first to it.third })
    }

    private fun createAnnotation(
        ctorSymbol: IrConstructorSymbol,
        ann: KmAnnotation,
        argSpecs: List<Pair<String, (Any?) -> IrExpression>>,
    ): IrAnnotation {
        val classType = IrSimpleTypeImpl(
            (ctorSymbol.owner.parent as IrClass).symbol,
            SimpleTypeNullability.NOT_SPECIFIED, emptyList(), emptyList()
        )
        val irAnnotation = IrAnnotationImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = classType,
            symbol = ctorSymbol,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
        )
        for ((index, spec) in argSpecs.withIndex()) {
            val (name, constFactory) = spec
            val kmValue = ann.arguments[name]
            val value = when (kmValue) {
                is KmAnnotationArgument.StringValue -> kmValue.value
                is KmAnnotationArgument.BooleanValue -> kmValue.value
                else -> null
            }
            irAnnotation.arguments[index] = constFactory(value)
        }
        return irAnnotation
    }

    private fun stringConst(value: Any?): IrExpression =
        IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, stringType(), (value as? String) ?: "")

    private fun booleanConst(value: Any?): IrExpression =
        IrConstImpl.boolean(UNDEFINED_OFFSET, UNDEFINED_OFFSET, booleanType(), (value as? Boolean) ?: false)

    fun convert() {
        // Pass 1: Create all class symbols
        for (fragment in module.fragments) {
            for (kmClass in fragment.classes) {
                classSymbols[kmClass.name] = IrClassSymbolImpl()
            }
        }

        // Pass 2: Create IR declarations (process classes sorted by nesting depth)
        for (fragment in module.fragments) {
            val packageFqn = fragment.fqName?.replace('/', '.') ?: ""
            val packageFragment = getOrCreatePackageFragment(packageFqn)

            val sortedClasses = fragment.classes.sortedBy { it.name.count { c -> c == '.' } }
            for (kmClass in sortedClasses) {
                createClassStub(kmClass, packageFragment)
            }

            fragment.pkg?.let { pkg ->
                val topLevelScope = TypeParameterScope(null)
                for (func in pkg.functions) {
                    val irFunc = createFunctionStub(func, packageFragment, containingClass = null, topLevelScope)
                    functionStubs[func] = irFunc
                    annotationsMap[irFunc] = func.annotations
                    attachObjCAnnotations(irFunc, func.annotations)
                    packageFragment.declarations.add(irFunc)
                }
                for (prop in pkg.properties) {
                    val irProp = createPropertyStub(prop, packageFragment, containingClass = null, topLevelScope)
                    propertyStubs[prop] = irProp
                    annotationsMap[irProp] = prop.annotations
                    attachObjCAnnotations(irProp, prop.annotations)
                    packageFragment.declarations.add(irProp)
                }
            }
        }
    }

    // ---- Package fragments ----

    fun getPackageFragment(fqn: String): IrExternalPackageFragment? = packageFragments[fqn]

    private fun getOrCreatePackageFragment(fqn: String): IrExternalPackageFragment {
        return packageFragments.getOrPut(fqn) {
            IrExternalPackageFragmentImpl(
                IrExternalPackageFragmentSymbolImpl(),
                FqName(fqn),
            )
        }
    }

    // ---- Class symbol resolution (including external) ----

    private fun getOrCreateClassSymbol(className: String): IrClassSymbol {
        classSymbols[className]?.let { return it }

        // External class (from another klib) — create a stub
        val symbol = IrClassSymbolImpl()
        val parentClassName = parentClassNameOf(className)
        val parent: IrDeclarationParent = if (parentClassName != null) {
            getOrCreateClassSymbol(parentClassName).owner
        } else {
            getOrCreatePackageFragment(classNameToPackageFqn(className))
        }

        irFactory.createClass(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
            name = Name.identifier(classNameToSimpleName(className)),
            visibility = DescriptorVisibilities.PUBLIC,
            symbol = symbol,
            kind = IrClassKind.CLASS,
            modality = Modality.FINAL,
        ).apply {
            this.parent = parent
        }

        classSymbols[className] = symbol
        return symbol
    }

    // ---- Class stub creation ----

    private fun createClassStub(kmClass: KmClass, packageFragment: IrExternalPackageFragment) {
        val classSymbol = classSymbols[kmClass.name]!!
        val parentClassName = parentClassNameOf(kmClass.name)
        val parentDecl: IrDeclarationParent = if (parentClassName != null) {
            classStubs[parentClassName] ?: classSymbols[parentClassName]!!.owner
        } else {
            packageFragment
        }

        val parentScope = if (parentClassName != null && kmClass.isInner) {
            classScopes[parentClassName]
        } else {
            null
        }

        val irClass = irFactory.createClass(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
            name = Name.identifier(classNameToSimpleName(kmClass.name)),
            visibility = kmClass.visibility.toDescriptorVisibility(),
            symbol = classSymbol,
            kind = kmClass.kind.toIrClassKind(),
            modality = kmClass.modality.toIrModality(),
            isInner = kmClass.isInner,
            isValue = kmClass.isValue,
            isFun = kmClass.isFunInterface,
        )
        irClass.parent = parentDecl
        annotationsMap[irClass] = kmClass.annotations

        // Create type parameter scope
        val classScope = TypeParameterScope(parentScope)

        // Create type parameters (two sub-passes: create, then set bounds)
        val classTypeParams = mutableListOf<IrTypeParameter>()
        for ((index, tp) in kmClass.typeParameters.withIndex()) {
            val tpSymbol = IrTypeParameterSymbolImpl()
            val irTp = irFactory.createTypeParameter(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.DEFINED,
                name = Name.identifier(tp.name),
                symbol = tpSymbol,
                variance = tp.variance.toIrVariance(),
                index = index,
                isReified = tp.isReified,
            )
            irTp.parent = irClass
            classTypeParams.add(irTp)
            classScope.register(tp.id, tpSymbol)
        }
        irClass.typeParameters = classTypeParams

        // Set type parameter upper bounds
        for ((index, tp) in kmClass.typeParameters.withIndex()) {
            classTypeParams[index].superTypes = tp.upperBounds.map { convertType(it, classScope) }
        }

        classScopes[kmClass.name] = classScope
        classStubs[kmClass.name] = irClass

        // Set super types
        irClass.superTypes = kmClass.supertypes.map { convertType(it, classScope) }

        // Create member declarations
        for (func in kmClass.functions) {
            val irFunc = createFunctionStub(func, irClass, kmClass, classScope)
            if (func.kind == MemberKind.FAKE_OVERRIDE) {
                irFunc.origin = IrDeclarationOrigin.FAKE_OVERRIDE
            }
            functionStubs[func] = irFunc
            annotationsMap[irFunc] = func.annotations
            attachObjCAnnotations(irFunc, func.annotations)
            irClass.declarations.add(irFunc)
        }

        for (ctor in kmClass.constructors) {
            val irCtor = createConstructorStub(ctor, irClass, kmClass, classScope)
            constructorStubs[ctor] = irCtor
            annotationsMap[irCtor] = ctor.annotations
            attachObjCAnnotations(irCtor, ctor.annotations)
            irClass.declarations.add(irCtor)
        }

        for (prop in kmClass.properties) {
            val irProp = createPropertyStub(prop, irClass, kmClass, classScope)
            if (prop.kind == MemberKind.FAKE_OVERRIDE) {
                irProp.origin = IrDeclarationOrigin.FAKE_OVERRIDE
            }
            propertyStubs[prop] = irProp
            annotationsMap[irProp] = prop.annotations
            attachObjCAnnotations(irProp, prop.annotations)
            irClass.declarations.add(irProp)
        }

        for (entry in kmClass.kmEnumEntries) {
            val irEntry = createEnumEntryStub(entry, irClass)
            enumEntryStubs[entry] = irEntry
            annotationsMap[irEntry] = entry.annotations
            irClass.declarations.add(irEntry)
        }
    }

    // ---- Function stub creation ----

    private fun createFunctionStub(
        func: KmFunction,
        parent: IrDeclarationParent,
        containingClass: KmClass?,
        parentScope: TypeParameterScope,
    ): IrSimpleFunction {
        val funcScope = TypeParameterScope(parentScope)

        val irFunc = irFactory.createSimpleFunction(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
            name = Name.identifier(func.name),
            visibility = func.visibility.toDescriptorVisibility(),
            isInline = func.isInline,
            isExpect = false,
            returnType = null,
            modality = func.modality.toIrModality(),
            symbol = IrSimpleFunctionSymbolImpl(),
            isTailrec = false,
            isSuspend = func.isSuspend,
            isOperator = false,
            isInfix = false,
        )
        irFunc.parent = parent

        // Type parameters
        createTypeParameters(func.typeParameters, irFunc, funcScope)

        // Parameters
        val params = mutableListOf<IrValueParameter>()

        // Dispatch receiver (only for instance methods of non-object classes)
        if (containingClass != null && !isObjectLike(containingClass)) {
            params.add(createValueParameter("\$this", createClassType(containingClass.name), IrParameterKind.DispatchReceiver))
        }

        // Context parameters
        for (cp in func.contextParameters) {
            params.add(createValueParameter(cp.name, convertType(cp.type, funcScope), IrParameterKind.Context))
        }

        // Extension receiver
        func.receiverParameterType?.let { receiverType ->
            params.add(createValueParameter("\$this\$${func.name}", convertType(receiverType, funcScope), IrParameterKind.ExtensionReceiver))
        }

        // Regular parameters
        for (vp in func.valueParameters) {
            params.add(createValueParameter(
                name = vp.name,
                type = convertType(vp.type, funcScope),
                kind = IrParameterKind.Regular,
                varargElementType = vp.varargElementType?.let { convertType(it, funcScope) },
                isCrossinline = vp.isCrossinline,
                isNoinline = vp.isNoinline,
            ))
        }

        irFunc.parameters = params
        irFunc.returnType = convertType(func.returnType, funcScope)

        return irFunc
    }

    // ---- Constructor stub creation ----

    private fun createConstructorStub(
        ctor: KmConstructor,
        parentClass: IrClass,
        containingKmClass: KmClass,
        classScope: TypeParameterScope,
    ): IrConstructor {
        val irCtor = irFactory.createConstructor(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
            name = Name.special("<init>"),
            visibility = ctor.visibility.toDescriptorVisibility(),
            isInline = false,
            isExpect = false,
            returnType = createClassType(containingKmClass.name),
            symbol = IrConstructorSymbolImpl(),
            isPrimary = false,
        )
        irCtor.parent = parentClass

        // Parameters
        val params = mutableListOf<IrValueParameter>()
        for (vp in ctor.valueParameters) {
            params.add(createValueParameter(
                name = vp.name,
                type = convertType(vp.type, classScope),
                kind = IrParameterKind.Regular,
                varargElementType = vp.varargElementType?.let { convertType(it, classScope) },
                isCrossinline = vp.isCrossinline,
                isNoinline = vp.isNoinline,
            ))
        }
        irCtor.parameters = params

        return irCtor
    }

    // ---- Property stub creation ----

    private fun createPropertyStub(
        prop: KmProperty,
        parent: IrDeclarationParent,
        containingClass: KmClass?,
        parentScope: TypeParameterScope,
    ): IrProperty {
        val propSymbol = IrPropertySymbolImpl()

        val irProp = irFactory.createProperty(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
            name = Name.identifier(prop.name),
            visibility = prop.visibility.toDescriptorVisibility(),
            modality = prop.modality.toIrModality(),
            symbol = propSymbol,
            isVar = prop.isVar,
            isConst = prop.isConst,
            isLateinit = false,
            isDelegated = false,
        )
        irProp.parent = parent

        // Getter
        val getter = createAccessorStub(
            prop, parent, containingClass, parentScope, propSymbol,
            isGetter = true,
        )
        irProp.getter = getter
        annotationsMap[getter] = prop.getter.annotations
        attachObjCAnnotations(getter, prop.getter.annotations)

        // Setter (only for var properties)
        if (prop.isVar) {
            val setter = createAccessorStub(
                prop, parent, containingClass, parentScope, propSymbol,
                isGetter = false,
            )
            irProp.setter = setter
            val setterAnnotations = prop.setter?.annotations.orEmpty()
            annotationsMap[setter] = setterAnnotations
            attachObjCAnnotations(setter, setterAnnotations)
        }

        return irProp
    }

    private fun createAccessorStub(
        prop: KmProperty,
        parent: IrDeclarationParent,
        containingClass: KmClass?,
        parentScope: TypeParameterScope,
        propSymbol: IrPropertySymbol,
        isGetter: Boolean,
    ): IrSimpleFunction {
        val accessorScope = TypeParameterScope(parentScope)
        val accessorName = if (isGetter) "<get-${prop.name}>" else "<set-${prop.name}>"
        val accessorAttrs = if (isGetter) prop.getter else prop.setter

        val accessor = irFactory.createSimpleFunction(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR,
            name = Name.special(accessorName),
            visibility = accessorAttrs?.visibility?.toDescriptorVisibility() ?: prop.visibility.toDescriptorVisibility(),
            isInline = accessorAttrs?.isInline ?: false,
            isExpect = false,
            returnType = null,
            modality = accessorAttrs?.modality?.toIrModality() ?: prop.modality.toIrModality(),
            symbol = IrSimpleFunctionSymbolImpl(),
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
        )
        accessor.parent = parent
        accessor.correspondingPropertySymbol = propSymbol

        // Type parameters
        createTypeParameters(prop.typeParameters, accessor, accessorScope)

        // Parameters
        val params = mutableListOf<IrValueParameter>()

        // Dispatch receiver
        if (containingClass != null && !isObjectLike(containingClass)) {
            params.add(createValueParameter("\$this", createClassType(containingClass.name), IrParameterKind.DispatchReceiver))
        }

        // Context parameters
        for (cp in prop.contextParameters) {
            params.add(createValueParameter(cp.name, convertType(cp.type, accessorScope), IrParameterKind.Context))
        }

        // Extension receiver
        prop.receiverParameterType?.let { receiverType ->
            params.add(createValueParameter("\$this\$${prop.name}", convertType(receiverType, accessorScope), IrParameterKind.ExtensionReceiver))
        }

        // Setter value parameter
        if (!isGetter) {
            val setterParam = prop.setterParameter
            params.add(createValueParameter(
                name = setterParam?.name ?: "<set-?>",
                type = convertType(setterParam?.type ?: prop.returnType, accessorScope),
                kind = IrParameterKind.Regular,
            ))
        }

        accessor.parameters = params
        accessor.returnType = if (isGetter) {
            convertType(prop.returnType, accessorScope)
        } else {
            createUnitType()
        }

        return accessor
    }

    // ---- Enum entry stub creation ----

    private fun createEnumEntryStub(entry: KmEnumEntry, parentClass: IrClass): IrEnumEntry {
        val irEntry = irFactory.createEnumEntry(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
            name = Name.identifier(entry.name),
            symbol = IrEnumEntrySymbolImpl(),
        )
        irEntry.parent = parentClass
        return irEntry
    }

    // ---- Type parameters ----

    private fun createTypeParameters(
        typeParams: List<KmTypeParameter>,
        parent: IrDeclaration,
        scope: TypeParameterScope,
    ) {
        val irParent = parent as IrTypeParametersContainer
        val irTypeParams = mutableListOf<IrTypeParameter>()
        for ((index, tp) in typeParams.withIndex()) {
            val tpSymbol = IrTypeParameterSymbolImpl()
            val irTp = irFactory.createTypeParameter(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.DEFINED,
                name = Name.identifier(tp.name),
                symbol = tpSymbol,
                variance = tp.variance.toIrVariance(),
                index = index,
                isReified = tp.isReified,
            )
            irTp.parent = parent as IrDeclarationParent
            irTypeParams.add(irTp)
            scope.register(tp.id, tpSymbol)
        }
        irParent.typeParameters = irTypeParams

        // Set upper bounds (separate pass since bounds may reference sibling type params)
        for ((index, tp) in typeParams.withIndex()) {
            irTypeParams[index].superTypes = tp.upperBounds.map { convertType(it, scope) }
        }
    }

    // ---- Value parameters ----

    private fun createValueParameter(
        name: String,
        type: IrType,
        kind: IrParameterKind,
        varargElementType: IrType? = null,
        isCrossinline: Boolean = false,
        isNoinline: Boolean = false,
    ): IrValueParameter {
        return irFactory.createValueParameter(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.DEFINED,
            kind = kind,
            name = Name.identifier(name),
            type = type,
            isAssignable = false,
            symbol = IrValueParameterSymbolImpl(),
            varargElementType = varargElementType,
            isCrossinline = isCrossinline,
            isNoinline = isNoinline,
            isHidden = false,
        )
    }

    // ---- Type conversion ----

    private fun convertType(kmType: KmType, scope: TypeParameterScope): IrType {
        val nullability = when {
            kmType.isDefinitelyNonNull -> SimpleTypeNullability.DEFINITELY_NOT_NULL
            kmType.isNullable -> SimpleTypeNullability.MARKED_NULLABLE
            else -> SimpleTypeNullability.NOT_SPECIFIED
        }
        return when (val classifier = kmType.classifier) {
            is KmClassifier.Class -> {
                val classSymbol = getOrCreateClassSymbol(classifier.name)
                val arguments = kmType.arguments.map { convertTypeArgument(it, scope) }
                IrSimpleTypeImpl(classSymbol, nullability, arguments, emptyList())
            }
            is KmClassifier.TypeParameter -> {
                val tpSymbol = scope.resolve(classifier.id)
                IrSimpleTypeImpl(tpSymbol, nullability, emptyList(), emptyList())
            }
            is KmClassifier.TypeAlias -> {
                val classSymbol = getOrCreateClassSymbol(classifier.name)
                val arguments = kmType.arguments.map { convertTypeArgument(it, scope) }
                IrSimpleTypeImpl(classSymbol, nullability, arguments, emptyList())
            }
        }
    }

    private fun convertTypeArgument(arg: KmTypeProjection, scope: TypeParameterScope): IrTypeArgument {
        if (arg == KmTypeProjection.STAR) return IrStarProjectionImpl
        val irType = convertType(arg.type!!, scope)
        return when (arg.variance!!) {
            KmVariance.INVARIANT -> irType as IrTypeArgument
            KmVariance.IN -> makeTypeProjection(irType, Variance.IN_VARIANCE)
            KmVariance.OUT -> makeTypeProjection(irType, Variance.OUT_VARIANCE)
        }
    }

    private fun createClassType(className: String): IrSimpleType {
        val classSymbol = getOrCreateClassSymbol(className)
        return IrSimpleTypeImpl(classSymbol, SimpleTypeNullability.NOT_SPECIFIED, emptyList(), emptyList())
    }

    private fun createUnitType(): IrSimpleType {
        val unitSymbol = getOrCreateClassSymbol("kotlin/Unit")
        return IrSimpleTypeImpl(unitSymbol, SimpleTypeNullability.NOT_SPECIFIED, emptyList(), emptyList())
    }

    // ---- Helpers ----

    private fun isObjectLike(kmClass: KmClass): Boolean =
        kmClass.kind == KmClassKind.OBJECT || kmClass.kind == KmClassKind.COMPANION_OBJECT

    // ---- Type parameter scope ----

    private class TypeParameterScope(private val parent: TypeParameterScope?) {
        private val idToSymbol = HashMap<Int, IrTypeParameterSymbol>()

        fun register(id: Int, symbol: IrTypeParameterSymbol) {
            idToSymbol[id] = symbol
        }

        fun resolve(id: Int): IrTypeParameterSymbol =
            idToSymbol[id] ?: parent?.resolve(id) ?: error("Cannot resolve type parameter id=$id")
    }

    companion object {
        private fun ClassId.toClassName(): String {
            val pkg = packageFqName.asString().replace('.', '/')
            return "$pkg/$relativeClassName"
        }

        private val OBJC_METHOD_CLASS_NAME = NativeStandardInteropNames.objCMethodClassId.toClassName()
        private val OBJC_CONSTRUCTOR_CLASS_NAME = NativeStandardInteropNames.objCConstructorClassId.toClassName()
        private val OBJC_FACTORY_CLASS_NAME = NativeStandardInteropNames.objCFactoryClassId.toClassName()
        private val OBJC_DIRECT_CLASS_NAME = NativeStandardInteropNames.objCDirectClassId.toClassName()

        private fun classNameToSimpleName(className: String): String {
            val dotIndex = className.lastIndexOf('.')
            if (dotIndex >= 0) return className.substring(dotIndex + 1)
            val slashIndex = className.lastIndexOf('/')
            if (slashIndex >= 0) return className.substring(slashIndex + 1)
            return className
        }

        private fun classNameToPackageFqn(className: String): String {
            val dotIndex = className.indexOf('.')
            val topLevelPart = if (dotIndex >= 0) className.substring(0, dotIndex) else className
            val slashIndex = topLevelPart.lastIndexOf('/')
            return if (slashIndex >= 0) topLevelPart.substring(0, slashIndex).replace('/', '.') else ""
        }

        private fun parentClassNameOf(className: String): String? {
            val dotIndex = className.lastIndexOf('.')
            return if (dotIndex >= 0) className.substring(0, dotIndex) else null
        }

        private fun KmClassKind.toIrClassKind(): IrClassKind = when (this) {
            KmClassKind.CLASS -> IrClassKind.CLASS
            KmClassKind.INTERFACE -> IrClassKind.INTERFACE
            KmClassKind.ENUM_CLASS -> IrClassKind.ENUM_CLASS
            KmClassKind.ENUM_ENTRY -> IrClassKind.ENUM_ENTRY
            KmClassKind.ANNOTATION_CLASS -> IrClassKind.ANNOTATION_CLASS
            KmClassKind.OBJECT -> IrClassKind.OBJECT
            KmClassKind.COMPANION_OBJECT -> IrClassKind.OBJECT
        }

        private fun KmVariance.toIrVariance(): Variance = when (this) {
            KmVariance.INVARIANT -> Variance.INVARIANT
            KmVariance.IN -> Variance.IN_VARIANCE
            KmVariance.OUT -> Variance.OUT_VARIANCE
        }

        private fun KmVisibility.toDescriptorVisibility(): DescriptorVisibility = when (this) {
            KmVisibility.PUBLIC -> DescriptorVisibilities.PUBLIC
            KmVisibility.PROTECTED -> DescriptorVisibilities.PROTECTED
            KmVisibility.INTERNAL -> DescriptorVisibilities.INTERNAL
            KmVisibility.PRIVATE -> DescriptorVisibilities.PRIVATE
            KmVisibility.PRIVATE_TO_THIS -> DescriptorVisibilities.PRIVATE_TO_THIS
            KmVisibility.LOCAL -> DescriptorVisibilities.LOCAL
        }

        private fun kotlin.metadata.Modality.toIrModality(): Modality = when (this) {
            kotlin.metadata.Modality.FINAL -> Modality.FINAL
            kotlin.metadata.Modality.OPEN -> Modality.OPEN
            kotlin.metadata.Modality.ABSTRACT -> Modality.ABSTRACT
            kotlin.metadata.Modality.SEALED -> Modality.SEALED
        }
    }
}
