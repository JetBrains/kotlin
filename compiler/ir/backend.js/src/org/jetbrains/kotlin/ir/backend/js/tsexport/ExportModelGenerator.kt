/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.tsexport

import org.jetbrains.kotlin.backend.common.report
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.correspondingEnumEntry
import org.jetbrains.kotlin.ir.backend.js.ir.*
import org.jetbrains.kotlin.ir.backend.js.lower.ES6_BOX_PARAMETER
import org.jetbrains.kotlin.ir.backend.js.lower.isBoxParameter
import org.jetbrains.kotlin.ir.backend.js.lower.isEs6ConstructorReplacement
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.js.common.makeValidES5Identifier
import org.jetbrains.kotlin.js.config.compileLongAsBigint
import org.jetbrains.kotlin.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.butIf
import org.jetbrains.kotlin.utils.addToStdlib.runIf

private const val magicPropertyName = "__doNotUseOrImplementIt"

class ExportModelGenerator(val context: JsIrBackendContext, val generateNamespacesForPackages: Boolean) {
    private val transitiveExportCollector = TransitiveExportCollector(context)

    fun generateExport(file: IrPackageFragment): List<ExportedDeclaration> {
        val namespaceFqName = file.packageFqName
        val exports = file.declarations.memoryOptimizedMapNotNull { declaration ->
            declaration.takeIf { it.couldBeConvertedToExplicitExport() != true }?.let(::exportDeclaration)
        }

        return when {
            exports.isEmpty() -> emptyList()
            !generateNamespacesForPackages || namespaceFqName.isRoot -> exports
            else -> listOf(ExportedNamespace(namespaceFqName.toString(), exports))
        }
    }

    private fun exportDeclaration(declaration: IrDeclaration): ExportedDeclaration? {
        val candidate = getExportCandidate(declaration) ?: return null
        if (!shouldDeclarationBeExportedImplicitlyOrExplicitly(candidate, context, declaration)) return null

        return when (candidate) {
            is IrSimpleFunction -> exportFunction(candidate)
            is IrProperty -> exportProperty(candidate)
            is IrClass -> exportClass(candidate)
            is IrField -> null
            else -> irError("Can't export declaration") {
                withIrEntry("candidate", candidate)
            }
        }?.withAttributesFor(candidate)
    }

    private fun exportClass(candidate: IrClass): ExportedDeclaration? {
        val superTypes = candidate.defaultType.collectSuperTransitiveHierarchy() + candidate.superTypes

        return if (candidate.isEnumClass) {
            exportEnumClass(candidate, superTypes)
        } else {
            exportOrdinaryClass(candidate, superTypes)
        }
    }


    private fun exportFunction(function: IrSimpleFunction, specializedType: ExportedType? = null): ExportedDeclaration? {
        return when (val exportability = function.exportability(context)) {
            is Exportability.NotNeeded, is Exportability.Implicit -> null
            is Exportability.Prohibited -> ErrorDeclaration(exportability.reason)
            is Exportability.Allowed -> {
                val parent = function.parent
                val realOverrideTarget = function.realOverrideTargetOrNull
                val isStatic = function.isStaticMethod
                ExportedFunction(
                    realOverrideTarget
                        ?.getJsSymbolForOverriddenDeclaration()
                        ?.let(ExportedFunctionName::WellKnownSymbol)
                        ?: ExportedFunctionName.Identifier(function.getExportedIdentifier()),
                    returnType = specializedType ?: exportType(function.returnType, function),
                    typeParameters = function.typeParameters.memoryOptimizedMap { exportTypeParameter(it, function) },
                    isMember = parent is IrClass,
                    isStatic = isStatic,
                    isAbstract = parent is IrClass && !parent.isInterface && function.modality == Modality.ABSTRACT,
                    isProtected = function.visibility == DescriptorVisibilities.PROTECTED,
                    parameters = function.nonDispatchParameters
                        .filter { it.shouldBeExported() }
                        .butIf(isStatic && function.parentClassOrNull?.isInner == true) {
                            // Remove $outer argument from secondary constructors of inner classes
                            it.drop(1)
                        }
                        .memoryOptimizedMap {
                            exportParameter(
                                it,
                                it.hasDefaultValue || realOverrideTarget?.parameters?.get(it.indexInParameters)?.hasDefaultValue == true
                            )
                        }
                )
            }
        }
    }

    private fun exportConstructor(constructor: IrConstructor, isReadOnlyPropertyForInnerClass: Boolean): ExportedConstructor? {
        if (!constructor.isPrimary) return null
        val constructedClass = constructor.constructedClass
        val visibility = when {
            constructedClass.isInner && !isReadOnlyPropertyForInnerClass -> when (constructedClass.modality) {
                // Inner classes should be constructed as `new outerClassValue.Inner()`
                // in JavaScript instead of `new OuterClass.Inner(outerClassValue)`.
                // The only time when you might actually want to call the real inner class
                // constructor is when you're inheriting from it.
                Modality.SEALED, Modality.FINAL -> ExportedVisibility.PRIVATE
                Modality.ABSTRACT, Modality.OPEN -> ExportedVisibility.PROTECTED
            }
            else -> constructor.exportedVisibility
        }
        val parameters = if (visibility == ExportedVisibility.PRIVATE) {
            // There is no point in generating private constructor parameters, since you can't call this constructor,
            // and it leaks implementation details.
            // We only generate a private constructor because otherwise there would be an implicit default public constructor,
            // which we don't want.
            emptyList()
        } else {
            constructor.nonDispatchParameters
                .filterNot { it.isBoxParameter }
                .memoryOptimizedMap { exportParameter(it, it.hasDefaultValue) }
        }
        return ExportedConstructor(
            parameters = parameters,
            visibility = visibility,
        )
    }

    private fun exportParameter(parameter: IrValueParameter, hasDefaultValue: Boolean): ExportedParameter {
        // Parameter names do not matter in d.ts files. They can be renamed as we like
        var parameterName = makeValidES5Identifier(parameter.name.asString(), withHash = false)
        if (parameterName in allReservedWords)
            parameterName = "_$parameterName"

        return ExportedParameter(
            parameterName,
            exportType(parameter.type, parameter),
            hasDefaultValue
        )
    }

    private val IrValueParameter.hasDefaultValue: Boolean
        get() = origin == JsLoweredDeclarationOrigin.JS_SHADOWED_DEFAULT_PARAMETER

    private fun exportProperty(property: IrProperty): ExportedDeclaration? {
        for (accessor in listOfNotNull(property.getter, property.setter)) {
            // Frontend will report an error on an attempt to export an extension property.
            // Just to be safe, filter out such properties here as well.
            if (accessor.parameters.any { it.kind == IrParameterKind.ExtensionReceiver })
                return null
            if (accessor.isFakeOverride && !accessor.isAllowedFakeOverriddenDeclaration(context)) {
                return null
            }
        }

        return exportPropertyUnsafely(property)
    }

    private fun exportPropertyUnsafely(
        property: IrProperty,
        specializeType: ExportedType? = null
    ): ExportedDeclaration {
        val parentClass = property.parent as? IrClass
        val isOptional = property.isEffectivelyExternal() &&
                property.parent is IrClass &&
                property.getter?.returnType?.isNullable() == true

        return ExportedProperty(
            name = property.getExportedIdentifier(),
            type = specializeType ?: exportType(property.getter!!.returnType, property),
            mutable = property.isVar,
            isMember = parentClass != null,
            isAbstract = parentClass?.isInterface == false && property.modality == Modality.ABSTRACT,
            isProtected = property.visibility == DescriptorVisibilities.PROTECTED,
            isField = parentClass?.isInterface == true,
            isObjectGetter = property.getter?.origin == JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION,
            isOptional = isOptional,
            isStatic = (property.getter ?: property.setter)?.isStaticMethodOfClass == true,
        )
    }

    private fun exportEnumEntry(field: IrField, enumEntries: Map<IrEnumEntry, Int>): ExportedProperty {
        val irEnumEntry = field.correspondingEnumEntry
            ?: irError("Unable to find enum entry") {
                withIrEntry("field", field)
            }

        val parentClass = field.parent as IrClass

        val ordinal = enumEntries.getValue(irEnumEntry)

        fun fakeProperty(name: String, type: ExportedType) =
            ExportedProperty(name = name, type = type, mutable = false, isMember = true)

        val nameProperty = fakeProperty(
            name = "name",
            type = ExportedType.LiteralType.StringLiteralType(irEnumEntry.name.asString()),
        )

        val ordinalProperty = fakeProperty(
            name = "ordinal",
            type = ExportedType.LiteralType.NumberLiteralType(ordinal),
        )

        val type = ExportedType.InlineInterfaceType(
            listOf(nameProperty, ordinalProperty)
        )

        return ExportedProperty(
            name = irEnumEntry.getExportedIdentifier(),
            type = ExportedType.IntersectionType(exportType(parentClass.defaultType), type),
            mutable = false,
            isMember = true,
            isStatic = true,
            isProtected = parentClass.visibility == DescriptorVisibilities.PROTECTED,
        ).withAttributesFor(irEnumEntry)
    }

    private fun exportDeclarationImplicitly(klass: IrClass, superTypes: Iterable<IrType>): ExportedDeclaration {
        val typeParameters = klass.typeParameters.memoryOptimizedMap { exportTypeParameter(it, klass) }
        val superInterfaces = superTypes
            .filter { (it.classifierOrFail.owner as? IrDeclaration)?.isExportedImplicitlyOrExplicitly(context) ?: false }
            .map { exportType(it) }
            .memoryOptimizedFilter { it !is ExportedType.ErrorType }

        val name = klass.getExportedIdentifier()
        val (members, nestedClasses) = exportClassDeclarations(klass, superTypes)
        return ExportedRegularClass(
            name = name,
            isInterface = true,
            isAbstract = false,
            isExternal = klass.isExternal,
            superClasses = emptyList(),
            superInterfaces = superInterfaces,
            typeParameters = typeParameters,
            members = members,
            nestedClasses = nestedClasses,
            originalClassId = klass.classId,
        )
    }

    private fun exportOrdinaryClass(klass: IrClass, superTypes: Iterable<IrType>): ExportedDeclaration? {
        when (val exportability = klass.exportability()) {
            is Exportability.Prohibited -> irError(exportability.reason) {
                withIrEntry("klass", klass)
            }
            Exportability.NotNeeded -> return null
            Exportability.Implicit -> return exportDeclarationImplicitly(klass, superTypes)
            Exportability.Allowed -> {}
        }

        val (members, nestedClasses) = exportClassDeclarations(klass, superTypes)

        return exportClass(
            klass,
            superTypes,
            members,
            nestedClasses
        )
    }

    private fun exportEnumClass(klass: IrClass, superTypes: Iterable<IrType>): ExportedDeclaration? {
        when (val exportability = klass.exportability()) {
            is Exportability.Prohibited -> irError(exportability.reason) {
                withIrEntry("klass", klass)
            }
            Exportability.NotNeeded -> return null
            Exportability.Implicit -> return exportDeclarationImplicitly(klass, superTypes)
            Exportability.Allowed -> {}
        }

        val enumEntries = klass
            .declarations
            .filterIsInstance<IrField>()
            .mapNotNull { it.correspondingEnumEntry }

        val enumEntriesToOrdinal: Map<IrEnumEntry, Int> =
            enumEntries
                .keysToMap(enumEntries::indexOf)

        val (members, nestedClasses) = exportClassDeclarations(klass, superTypes) { candidate ->
            val enumExportedMember = exportAsEnumMember(candidate, enumEntriesToOrdinal)
            enumExportedMember
        }

        val privateConstructor = ExportedConstructor(
            parameters = emptyList(),
            visibility = ExportedVisibility.PRIVATE
        )

        return exportClass(
            klass,
            superTypes,
            listOf(privateConstructor) memoryOptimizedPlus members,
            nestedClasses
        )
    }

    private fun exportClassDeclarations(
        klass: IrClass,
        superTypes: Iterable<IrType>,
        specialProcessing: (IrDeclarationWithName) -> ExportedDeclaration? = { null }
    ): ExportedClassDeclarationsInfo {
        val members = mutableListOf<ExportedDeclaration>()
        val specialMembers = mutableListOf<ExportedDeclaration>()
        val nestedClasses = mutableListOf<ExportedClass>()
        val isImplicitlyExportedClass = klass.isJsImplicitExport()

        for (declaration in klass.declarations) {
            val candidate = getExportCandidate(declaration) ?: continue
            if (isImplicitlyExportedClass && candidate !is IrClass) continue
            if (!shouldDeclarationBeExportedImplicitlyOrExplicitly(candidate, context, declaration)) continue
            if (candidate.isFakeOverride && klass.isInterface) continue

            val processingResult = specialProcessing(candidate)
            if (processingResult != null) {
                specialMembers.add(processingResult)
                continue
            }

            when (candidate) {
                is IrSimpleFunction ->
                    members.addIfNotNull(exportFunction(candidate)?.withAttributesFor(candidate))

                is IrConstructor ->
                    members.addIfNotNull(
                        exportConstructor(
                            candidate,
                            isReadOnlyPropertyForInnerClass = false
                        )?.withAttributesFor(candidate)
                    )

                is IrProperty ->
                    members.addIfNotNull(exportProperty(candidate)?.withAttributesFor(candidate))

                is IrClass -> {
                    if (klass.isInterface && !candidate.isCompanion) continue
                    if (candidate.isInner) {
                        members.add(candidate.toReadOnlyPropertyForInnerClass().withAttributesFor(candidate))
                    }
                    val ec = exportClass(candidate)?.withAttributesFor(candidate)
                    if (ec is ExportedClass) {
                        nestedClasses.add(ec)
                    } else {
                        members.addIfNotNull(ec)
                    }
                }

                is IrField -> {
                    assert(
                        candidate.origin == IrDeclarationOrigin.FIELD_FOR_OUTER_THIS ||
                                candidate.correspondingPropertySymbol != null
                    ) {
                        "Unexpected field without property ${candidate.fqNameWhenAvailable}"
                    }
                }

                else -> irError("Can't export member declaration") {
                    withIrEntry("declaration", declaration)
                }
            }
        }

        if (klass.shouldContainImplementationOfMagicProperty(superTypes)) {
            members.addMagicPropertyForInterfaceImplementation(klass, superTypes)
        } else if (klass.shouldNotBeImplemented()) {
            members.addMagicInterfaceProperty(klass)
        }

        return ExportedClassDeclarationsInfo(
            specialMembers + members,
            nestedClasses
        )
    }

    private fun IrClass.toReadOnlyPropertyForInnerClass(): ExportedProperty {
        val innerClassReference = typeScriptInnerClassReference()
        val allPublicConstructors = constructors
            .mapNotNull { exportConstructor(it, isReadOnlyPropertyForInnerClass = true) }
            .filterNot { it.isProtected }
            .map {
                ExportedConstructSignature(
                    parameters = it.parameters.drop(1),
                    returnType = ExportedType.TypeParameter(innerClassReference),
                )
            }
            .toList()

        val type = ExportedType.IntersectionType(
            ExportedType.InlineInterfaceType(allPublicConstructors),
            ExportedType.TypeOf(
                ExportedType.ClassType(
                    innerClassReference,
                    emptyList(),
                    isObject = false,
                    isExternal,
                    classId,
                )
            )
        )

        val name = getExportedIdentifier()

        return ExportedProperty(name = name, type = type, mutable = false, isMember = true)
    }

    private fun IrClass.shouldNotBeImplemented(): Boolean {
        return isInterface && !isExternal || isJsImplicitExport()
    }

    private fun IrValueParameter.shouldBeExported(): Boolean {
        return origin != JsLoweredDeclarationOrigin.JS_SUPER_CONTEXT_PARAMETER && origin != ES6_BOX_PARAMETER
    }

    private fun IrClass.shouldContainImplementationOfMagicProperty(superTypes: Iterable<IrType>): Boolean {
        return !isExternal && superTypes.any {
            val superClass = it.classOrNull?.owner ?: return@any false
            superClass.isInterface && it.shouldAddMagicPropertyOfSuper() || superClass.isJsImplicitExport()
        }
    }

    private fun MutableList<ExportedDeclaration>.addMagicInterfaceProperty(klass: IrClass) {
        add(ExportedProperty(name = magicPropertyName, type = klass.generateTagType(), mutable = false, isMember = true, isField = true))
    }

    private fun MutableList<ExportedDeclaration>.addMagicPropertyForInterfaceImplementation(klass: IrClass, superTypes: Iterable<IrType>) {
        val allSuperTypesWithMagicProperty = superTypes.filter { it.shouldAddMagicPropertyOfSuper() }

        if (allSuperTypesWithMagicProperty.isEmpty()) {
            return
        }

        val intersectionOfTypes = allSuperTypesWithMagicProperty
            .map { ExportedType.PropertyType(exportType(it), ExportedType.LiteralType.StringLiteralType(magicPropertyName)) }
            .reduce(ExportedType::IntersectionType)
            .let { if (klass.shouldNotBeImplemented()) ExportedType.IntersectionType(klass.generateTagType(), it) else it }

        add(ExportedProperty(name = magicPropertyName, type = intersectionOfTypes, mutable = false, isMember = true, isField = true))
    }

    private fun IrType.shouldAddMagicPropertyOfSuper(): Boolean {
        return classOrNull?.owner?.isOwnMagicPropertyAdded() ?: false
    }

    private fun IrClass.isOwnMagicPropertyAdded(): Boolean {
        if (isJsImplicitExport()) return true
        if (!isExported(context)) return false
        return isInterface && !isExternal || superTypes.any {
            it.classOrNull?.owner?.isOwnMagicPropertyAdded() == true
        }
    }

    private fun IrClass.generateTagType(): ExportedType {
        return ExportedType.InlineInterfaceType(
            listOf(
                ExportedProperty(
                    name = getFqNameWithJsNameWhenAvailable(true).asString(),
                    type = ExportedType.Primitive.UniqueSymbol,
                    mutable = false,
                    isMember = true,
                    isField = true,
                )
            )
        )
    }

    private fun exportClass(
        klass: IrClass,
        superTypes: Iterable<IrType>,
        members: List<ExportedDeclaration>,
        nestedClasses: List<ExportedClass>,
    ): ExportedDeclaration {
        val typeParameters = klass.typeParameters.memoryOptimizedMap { exportTypeParameter(it, klass) }

        val superClasses = superTypes
            .filter { !it.classifierOrFail.isInterface && it.canBeUsedAsSuperTypeOfExportedClasses() }
            .map { exportType(it, shouldCalculateExportedSupertypeForImplicit = false) }
            .memoryOptimizedFilter { it !is ExportedType.ErrorType }

        val superInterfaces = superTypes
            .filter { it.shouldPresentInsideImplementsClause() }
            .map { exportType(it, shouldCalculateExportedSupertypeForImplicit = false) }
            .memoryOptimizedFilter { it !is ExportedType.ErrorType }

        val name = klass.getExportedIdentifier()

        return if (klass.kind == ClassKind.OBJECT) {
            return ExportedObject(
                name = name,
                members = members,
                superClasses = superClasses,
                nestedClasses = nestedClasses,
                superInterfaces = superInterfaces,
                originalClassId = klass.classId,
                isCompanion = klass.isCompanion,
                isExternal = klass.isExternal,
                isTopLevel = klass.isTopLevel
            )
        } else {
            ExportedRegularClass(
                name = name,
                isInterface = klass.isInterface,
                isAbstract = klass.modality == Modality.ABSTRACT || klass.modality == Modality.SEALED || klass.isEnumClass,
                isExternal = klass.isExternal,
                superClasses = superClasses,
                superInterfaces = superInterfaces,
                typeParameters = typeParameters,
                members = members,
                nestedClasses = nestedClasses,
                originalClassId = klass.classId,
            )
        }
    }

    private fun IrSimpleType.collectSuperTransitiveHierarchy(): Set<IrType> =
        transitiveExportCollector.collectSuperTypesTransitiveHierarchyFor(this)

    private fun IrType.shouldPresentInsideImplementsClause(): Boolean {
        val classifier = classifierOrFail
        return classifier.isInterface || (classifier.owner as? IrDeclaration)?.isJsImplicitExport() == true
    }

    private fun exportAsEnumMember(
        candidate: IrDeclarationWithName,
        enumEntriesToOrdinal: Map<IrEnumEntry, Int>
    ): ExportedDeclaration? {
        val enumEntries = enumEntriesToOrdinal.keys
        return when (candidate) {
            is IrSimpleFunction if candidate.origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER -> {
                val specializedType = when (candidate.name) {
                    StandardNames.ENUM_VALUES -> ExportedType.InlineArrayType(
                        enumEntriesToOrdinal.keys
                            .map {
                                ExportedType.TypeOf(
                                    ExportedType.ClassType(
                                        name = it.getFqNameWithJsNameWhenAvailable(generateNamespacesForPackages).asString(),
                                        arguments = emptyList()
                                    )
                                )
                            }
                    )
                    StandardNames.ENUM_VALUE_OF if enumEntriesToOrdinal.isEmpty() -> ExportedType.Primitive.Nothing
                    else -> null
                }
                exportFunction(candidate, specializedType)
            }
            is IrProperty -> {
                if (candidate.isAllowedFakeOverriddenDeclaration(context)) {
                    val type: ExportedType = when (candidate.getExportedIdentifier()) {
                        "name" -> enumEntries
                            .map { it.name.asString() }
                            .map { ExportedType.LiteralType.StringLiteralType(it) }
                            .reduceOrNull { acc: ExportedType, s: ExportedType -> ExportedType.UnionType(acc, s) }
                            ?: ExportedType.Primitive.Nothing
                        "ordinal" -> enumEntriesToOrdinal
                            .map { (_, ordinal) -> ExportedType.LiteralType.NumberLiteralType(ordinal) }
                            .reduceOrNull { acc: ExportedType, s: ExportedType -> ExportedType.UnionType(acc, s) }
                            ?: ExportedType.Primitive.Nothing
                        else -> return null
                    }
                    exportPropertyUnsafely(
                        candidate,
                        type
                    )
                } else null
            }

            is IrField -> {
                if (candidate.origin == IrDeclarationOrigin.FIELD_FOR_ENUM_ENTRY) {
                    exportEnumEntry(candidate, enumEntriesToOrdinal)
                } else {
                    null
                }
            }

            else -> null
        }
    }

    private fun IrType.canBeUsedAsSuperTypeOfExportedClasses(): Boolean =
        !isAny() &&
                classifierOrNull != context.irBuiltIns.enumClass &&
                (classifierOrNull?.owner as? IrDeclaration)?.isJsImplicitExport() != true

    private fun exportTypeArgument(type: IrTypeArgument, typeOwner: IrDeclaration?): ExportedType {
        if (type is IrTypeProjection)
            return exportType(type.type, typeOwner)

        return ExportedType.ErrorType("UnknownType ${type.render()}")
    }

    fun exportTypeParameter(typeParameter: IrTypeParameter, typeOwner: IrDeclaration?): ExportedType.TypeParameter {
        val constraint = typeParameter.superTypes.asSequence()
            .filter { it != context.irBuiltIns.anyNType }
            .map {
                val exportedType = exportType(it, typeOwner)
                if (exportedType is ExportedType.ImplicitlyExportedType && exportedType.exportedSupertype == ExportedType.Primitive.Any) {
                    exportedType.copy(exportedSupertype = ExportedType.Primitive.Unknown)
                } else {
                    exportedType
                }
            }
            .filter { it !is ExportedType.ErrorType }
            .toList()

        return ExportedType.TypeParameter(
            typeParameter.name.identifier,
            constraint.run {
                when (size) {
                    0 -> null
                    1 -> single()
                    else -> reduce(ExportedType::IntersectionType)
                }
            }
        )
    }

    private val currentlyProcessedTypes = hashSetOf<IrType>()

    private fun exportType(
        type: IrType,
        typeOwner: IrDeclaration? = null,
        shouldCalculateExportedSupertypeForImplicit: Boolean = true,
    ): ExportedType {
        if (type is IrDynamicType || type in currentlyProcessedTypes)
            return ExportedType.Primitive.Any

        if (type !is IrSimpleType)
            return ExportedType.ErrorType("NonSimpleType ${type.render()}")

        currentlyProcessedTypes.add(type)

        val classifier = type.classifier
        val isMarkedNullable = type.isMarkedNullable()
        val nonNullType = type.makeNotNull() as IrSimpleType

        val exportedType = when {
            nonNullType.isBoolean() -> ExportedType.Primitive.Boolean
            nonNullType.isLong() || nonNullType.isULong() -> {
                if (!context.configuration.compileLongAsBigint) {
                    context.report(
                        CompilerMessageSeverity.ERROR,
                        typeOwner,
                        typeOwner?.file,
                        "Long can't be exported without using of the bigint type. Add -Xes-long-as-bigint compiler argument or set target to 'es2015'"
                    )
                }
                ExportedType.Primitive.BigInt
            }
            nonNullType.isPrimitiveType() && !nonNullType.isChar() ->
                ExportedType.Primitive.Number

            nonNullType.isByteArray() -> ExportedType.Primitive.ByteArray
            nonNullType.isShortArray() -> ExportedType.Primitive.ShortArray
            nonNullType.isIntArray() -> ExportedType.Primitive.IntArray
            nonNullType.isFloatArray() -> ExportedType.Primitive.FloatArray
            nonNullType.isDoubleArray() -> ExportedType.Primitive.DoubleArray
            nonNullType.isLongArray() -> when {
                context.configuration.compileLongAsBigint -> ExportedType.Primitive.LongArray
                else -> ExportedType.ErrorType("LongArray")
            }

            // TODO: Cover these in frontend
            nonNullType.isBooleanArray() -> ExportedType.ErrorType("BooleanArray")
            nonNullType.isCharArray() -> ExportedType.ErrorType("CharArray")

            nonNullType.isString() -> ExportedType.Primitive.String
            nonNullType.isThrowable() -> ExportedType.Primitive.Throwable
            nonNullType.isAny() -> ExportedType.Primitive.Any  // TODO: Should we wrap Any in a Nullable type?
            nonNullType.isUnit() -> ExportedType.Primitive.Unit
            nonNullType.isNothing() -> ExportedType.Primitive.Nothing
            nonNullType.isArray() -> ExportedType.Array(exportTypeArgument(nonNullType.arguments[0], typeOwner))
            nonNullType.isSuspendFunction() -> ExportedType.ErrorType("Suspend functions are not supported")
            nonNullType.isFunction() -> ExportedType.Function(
                parameters = nonNullType.arguments.dropLast(1).memoryOptimizedMap {
                    ExportedParameter(
                        name = (it as? IrTypeProjection)?.type?.getAnnotationArgumentValue(StandardNames.FqNames.parameterName, "name"),
                        type = exportTypeArgument(it, typeOwner),
                    )
                },
                returnType = exportTypeArgument(nonNullType.arguments.last(), typeOwner)
            )

            classifier is IrTypeParameterSymbol -> ExportedType.TypeParameter(classifier.owner.name.identifier)

            classifier is IrClassSymbol -> {
                val klass = classifier.owner
                val isExported = klass.isExportedImplicitlyOrExplicitly(context)
                val isImplicitlyExported = !isExported && !klass.isExternal
                val isNonExportedExternal = klass.isExternal && !isExported
                val name = klass.getFqNameWithJsNameWhenAvailable(!isNonExportedExternal && generateNamespacesForPackages).asString()

                val exportedSupertype = runIf(shouldCalculateExportedSupertypeForImplicit && isImplicitlyExported) {
                    val transitiveExportedType = nonNullType.collectSuperTransitiveHierarchy()
                    if (transitiveExportedType.isEmpty()) return@runIf null
                    transitiveExportedType
                        .memoryOptimizedMap { exportType(it, typeOwner) }
                        .reduce(ExportedType::IntersectionType)
                } ?: ExportedType.Primitive.Any

                when (klass.kind) {
                    ClassKind.ANNOTATION_CLASS,
                    ClassKind.ENUM_ENTRY ->
                        ExportedType.ErrorType("Class $name with kind: ${klass.kind}")

                    ClassKind.OBJECT ->
                        ExportedType.TypeOf(
                            ExportedType.ClassType(
                                name,
                                emptyList(),
                                isObject = true,
                                isExternal = klass.isEffectivelyExternal(),
                                classId = klass.classId,
                            )
                        )

                    ClassKind.CLASS,
                    ClassKind.ENUM_CLASS,
                    ClassKind.INTERFACE -> ExportedType.ClassType(
                        name,
                        type.arguments.memoryOptimizedMap { exportTypeArgument(it, typeOwner) },
                        isObject = false,
                        isExternal = klass.isEffectivelyExternal(),
                        classId = klass.classId,
                    )
                }.withImplicitlyExported(isImplicitlyExported, exportedSupertype)
            }

            else -> irError("Unexpected classifier") {
                withIrEntry("classifier.owner", classifier.owner)
            }
        }

        return exportedType.withNullability(isMarkedNullable)
            .also { currentlyProcessedTypes.remove(type) }
    }
}

private class ExportedClassDeclarationsInfo(
    val members: List<ExportedDeclaration>,
    val nestedClasses: List<ExportedClass>
) {
    operator fun component1() = members
    operator fun component2() = nestedClasses
}

private val IrClassifierSymbol.isInterface
    get() = (owner as? IrClass)?.isInterface == true

private val IrFunction.isStaticMethod: Boolean
    get() = isEs6ConstructorReplacement || isStaticMethodOfClass

private fun IrDeclaration.isExportedImplicitlyOrExplicitly(context: JsIrBackendContext): Boolean {
    val candidate = getExportCandidate(this) ?: return false
    return shouldDeclarationBeExportedImplicitlyOrExplicitly(candidate, context, this)
}

fun DescriptorVisibility.toExportedVisibility() =
    when (this) {
        DescriptorVisibilities.PROTECTED -> ExportedVisibility.PROTECTED
        else -> ExportedVisibility.DEFAULT
    }

private fun <T : ExportedDeclaration> T.withAttributesFor(declaration: IrDeclaration): T {
    declaration.getDeprecated()?.let { attributes.add(ExportedAttribute.DeprecatedAttribute(it)) }

    if (declaration.isJsExportDefault()) {
        attributes.add(ExportedAttribute.DefaultExport)
    }

    return this
}

