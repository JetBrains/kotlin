/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.tsexport

import org.jetbrains.kotlin.backend.common.report
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
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
import org.jetbrains.kotlin.ir.backend.js.lower.isExportedDefaultImplementation
import org.jetbrains.kotlin.ir.backend.js.lower.isEs6ConstructorReplacement
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.js.common.makeValidES5Identifier
import org.jetbrains.kotlin.js.config.compileLongAsBigint
import org.jetbrains.kotlin.js.util.NameTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.butIf
import org.jetbrains.kotlin.utils.addToStdlib.runIf

private const val ownImplementableSymbolName = "Symbol"
private const val notImplementablePropertyName = "__doNotUseOrImplementIt"

class ExportModelGenerator(val context: JsIrBackendContext, val isEsModules: Boolean) {
    private val transitiveExportCollector = TransitiveExportCollector(context)
    private val allowImplementingInterfaces = context.configuration.languageVersionSettings.supportsFeature(
        LanguageFeature.JsExportInterfacesInImplementableWay
    )

    // Some declarations are exported in a special form that even with [LanguageFeature.JsExportInterfacesInImplementableWay]
    // can't be implementable. As for example, any exported Kotlin collections right now can't be implemented on the JS / TS side
    private val packagesThatAreNotImplementable: Set<FqName> = hashSetOf(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME)

    private fun IrClass.shouldContainImplementableSymbolProperty(): Boolean =
        allowImplementingInterfaces && isInterface && !isExternal && !isJsImplicitExport() && !isJsNoRuntime() &&
                fileOrNull?.packageFqName !in packagesThatAreNotImplementable

    private fun IrClass.shouldContainNotImplementableProperty(): Boolean =
        isJsImplicitExport() ||
                fileOrNull?.packageFqName in packagesThatAreNotImplementable ||
                (!allowImplementingInterfaces && isInterface && !isExternal && !isJsNoRuntime())

    fun generateExport(file: IrPackageFragment): List<ExportedDeclaration> {
        val namespaceFqName = file.packageFqName
        val exports = file.declarations.memoryOptimizedMapNotNull { declaration ->
            declaration.takeIf { it.couldBeConvertedToExplicitExport() != true }?.let(::exportDeclaration)
        }

        return when {
            exports.isEmpty() -> emptyList()
            isEsModules || namespaceFqName.isRoot -> exports
            else -> listOf(ExportedNamespace(namespaceFqName.toString(), exports))
        }
    }

    private fun exportDeclaration(declaration: IrDeclaration): ExportedDeclaration? {
        val candidate = getExportCandidate(declaration) ?: return null
        if (!shouldDeclarationBeExportedImplicitlyOrExplicitly(candidate, context, declaration)) return null

        return when (candidate) {
            is IrSimpleFunction -> exportFunction(candidate, emptyMap())
            is IrProperty -> exportProperty(candidate, emptyMap())
            is IrClass -> exportClass(candidate, emptyMap())
            is IrField -> null
            else -> irError("Can't export declaration") {
                withIrEntry("candidate", candidate)
            }
        }?.withAttributesFor(candidate)
    }

    private fun exportClass(candidate: IrClass, outerClassTypeParameterScope: TypeParameterScope): ExportedDeclaration? {
        val superTypes = candidate.defaultType.collectSuperTransitiveHierarchy() + candidate.superTypes

        return if (candidate.isEnumClass) {
            exportEnumClass(candidate, superTypes)
        } else {
            exportOrdinaryClass(candidate, superTypes, outerClassTypeParameterScope)
        }
    }


    private fun exportFunction(
        function: IrSimpleFunction,
        classTypeParameterScope: TypeParameterScope,
        specializedType: ExportedType? = null,
        specializedName: String? = null,
        isFactoryPropertyForInnerClass: Boolean = false,
    ): ExportedDeclaration? {
        return when (val exportability = function.exportability(context, specializedName)) {
            is Exportability.NotNeeded, is Exportability.Implicit -> null
            is Exportability.Prohibited -> ErrorDeclaration(exportability.reason)
            is Exportability.Allowed -> {
                val parent = function.parent
                val realOverrideTarget = function.realOverrideTargetOrNull
                val isStatic = function.isStaticMethod
                val isExportedDefaultImplementation = function.isExportedDefaultImplementation
                val isInnerClassMember = parent is IrClass && parent.isInner
                if (isStatic && isInnerClassMember && !isFactoryPropertyForInnerClass) {
                    // Static members of inner classes should only be generated in the corresponding factory property of the outer class
                    return null
                }
                val outerScope = if (!isStatic || isFactoryPropertyForInnerClass) classTypeParameterScope else emptyMap()
                val functionTypeParameterScope = newTypeParameterScope(function, outerScope)
                ExportedFunction(
                    specializedName?.let(ExportedMemberName::Identifier)
                        ?: realOverrideTarget
                            ?.getJsSymbolForOverriddenDeclaration()
                            ?.let(ExportedMemberName::WellKnownSymbol)
                        ?: ExportedMemberName.Identifier(function.getExportedIdentifier()),
                    returnType = specializedType ?: exportType(function.returnType, functionTypeParameterScope, function),
                    typeParameters = function.typeParameters.map { functionTypeParameterScope[it.symbol]!! },
                    isMember = parent is IrClass && !isExportedDefaultImplementation,
                    isStatic = isStatic && !isFactoryPropertyForInnerClass && !isExportedDefaultImplementation,
                    isAbstract = parent is IrClass && !parent.isInterface && function.modality == Modality.ABSTRACT,
                    isProtected = function.visibility == DescriptorVisibilities.PROTECTED,
                    parameters = function.nonDispatchParameters
                        .filter { it.shouldBeExported() }
                        .butIf(isStatic && isInnerClassMember) {
                            // Remove $outer argument from secondary constructors of inner classes
                            it.drop(1)
                        }
                        .memoryOptimizedMap {
                            exportParameter(
                                it,
                                it.hasDefaultValue || realOverrideTarget?.parameters?.get(it.indexInParameters)?.hasDefaultValue == true,
                                functionTypeParameterScope,
                            )
                        }
                )
            }
        }
    }

    private fun exportConstructor(
        constructor: IrConstructor,
        typeParameterScope: TypeParameterScope,
        isFactoryPropertyForInnerClass: Boolean,
    ): ExportedConstructor? {
        if (!constructor.isPrimary) return null
        val constructedClass = constructor.constructedClass
        val visibility = when {
            constructedClass.isInner && !isFactoryPropertyForInnerClass -> when (constructedClass.modality) {
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
                .memoryOptimizedMap { exportParameter(it, it.hasDefaultValue, typeParameterScope) }
        }
        return ExportedConstructor(
            parameters = parameters,
            visibility = visibility,
        )
    }

    private fun exportParameter(
        parameter: IrValueParameter,
        hasDefaultValue: Boolean,
        typeParameterScope: TypeParameterScope,
    ): ExportedParameter {
        // Parameter names do not matter in d.ts files. They can be renamed as we like
        var parameterName = makeValidES5Identifier(parameter.name.asString(), withHash = false)
        if (parameterName in allReservedWords)
            parameterName = "_$parameterName"

        return ExportedParameter(
            parameterName,
            exportType(parameter.type, typeParameterScope, parameter),
            hasDefaultValue
        )
    }

    private val IrValueParameter.hasDefaultValue: Boolean
        get() = origin == JsLoweredDeclarationOrigin.JS_SHADOWED_DEFAULT_PARAMETER

    private fun exportProperty(
        property: IrProperty,
        classTypeParameterScope: TypeParameterScope,
        isFactoryPropertyForInnerClass: Boolean = false,
    ): ExportedDeclaration? {
        for (accessor in listOfNotNull(property.getter, property.setter)) {
            // Frontend will report an error on an attempt to export an extension property.
            // Just to be safe, filter out such properties here as well.
            if (accessor.parameters.any { it.kind == IrParameterKind.ExtensionReceiver })
                return null
            if (accessor.isFakeOverride && !accessor.isAllowedFakeOverriddenDeclaration(context)) {
                return null
            }
        }

        return exportPropertyUnsafely(property, classTypeParameterScope, isFactoryPropertyForInnerClass = isFactoryPropertyForInnerClass)
    }

    private fun exportPropertyUnsafely(
        property: IrProperty,
        classTypeParameterScope: TypeParameterScope,
        specializeType: ExportedType? = null,
        isFactoryPropertyForInnerClass: Boolean = false,
    ): ExportedDeclaration? {
        val parentClass = property.parent as? IrClass
        val isOptional = property.isEffectivelyExternal() &&
                property.parent is IrClass &&
                property.getter?.returnType?.isNullable() == true

        val isStatic = property.isStaticProperty
        val isExportedDefaultImplementation = property.isExportedDefaultImplementation
        if (isStatic && property.parentClassOrNull?.isInner == true && !isFactoryPropertyForInnerClass) {
            // Static members of inner classes should only be generated in the corresponding factory property of the outer class
            return null
        }

        val isPropertyAMember = parentClass != null && !isExportedDefaultImplementation
        val shouldPropertyBeStatic = isStatic && !isFactoryPropertyForInnerClass && !isExportedDefaultImplementation
        val isObjectGetter = property.getter?.origin == JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION
        val shouldBeExportedAsObjectWithAccessorsInside =
            (isExportedDefaultImplementation || isEsModules) && !isPropertyAMember && !shouldPropertyBeStatic

        val propertyType = specializeType
            ?: runIf(shouldBeExportedAsObjectWithAccessorsInside) {
                when {
                    isObjectGetter -> (exportFunction(
                        property.getter!!, emptyMap(), specializedName = "getInstance"
                    ) as ExportedFunction).copy(isMember = true).let { ExportedType.InlineInterfaceType(listOf(it)) }
                    else -> ExportedType.InlineInterfaceType(
                        listOfNotNull(
                            property.getter?.let { exportFunction(it, emptyMap(), specializedName = "get") as ExportedFunction }
                                ?.copy(isMember = true),
                            property.setter?.let { exportFunction(it, emptyMap(), specializedName = "set") as ExportedFunction }
                                ?.copy(isMember = true),
                        ))
                }
            } ?: exportType(property.getter!!.returnType, classTypeParameterScope, property)

        return ExportedProperty(
            name = ExportedMemberName.Identifier(property.getExportedIdentifier()),
            type = propertyType,
            mutable = property.isVar && !shouldBeExportedAsObjectWithAccessorsInside,
            isMember = parentClass != null && !isExportedDefaultImplementation,
            isStatic = shouldPropertyBeStatic,
            isAbstract = parentClass?.isInterface == false && property.modality == Modality.ABSTRACT,
            isProtected = property.visibility == DescriptorVisibilities.PROTECTED,
            isField = parentClass?.isInterface == true,
            isObjectGetter = isObjectGetter,
            isOptional = isOptional,
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
            ExportedProperty(name = ExportedMemberName.Identifier(name), type = type, mutable = false, isMember = true)

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
            name = ExportedMemberName.Identifier(irEnumEntry.getExportedIdentifier()),
            type = ExportedType.IntersectionType(exportType(parentClass.defaultType, emptyMap()), type),
            mutable = false,
            isMember = true,
            isStatic = true,
            isProtected = parentClass.visibility == DescriptorVisibilities.PROTECTED,
        ).withAttributesFor(irEnumEntry)
    }

    private fun exportDeclarationImplicitly(
        klass: IrClass,
        superTypes: Iterable<IrType>,
        outerClassTypeParameterScope: TypeParameterScope,
    ): ExportedDeclaration {
        val typeParameterScope = newTypeParameterScope(klass, outerClassTypeParameterScope, renameOuterTypeParameters = true)
        val name = klass.getExportedIdentifier()
        val superInterfaces = superTypes
            .filter { (it.classifierOrFail.owner as? IrDeclaration)?.isExportedImplicitlyOrExplicitly(context) ?: false }
            .map { exportType(it, typeParameterScope) }
            .memoryOptimizedFilter { it !is ExportedType.ErrorType }
        val (members, nestedClasses) = exportClassDeclarations(klass, superTypes, typeParameterScope)
        return ExportedRegularClass(
            name = name,
            isInterface = true,
            isAbstract = false,
            isExternal = klass.isExternal,
            superClasses = emptyList(),
            superInterfaces = superInterfaces,
            typeParameters = typeParameterScope.values.toList(),
            members = members,
            nestedClasses = nestedClasses,
            originalClassId = klass.classId,
        )
    }

    private fun exportOrdinaryClass(
        klass: IrClass,
        superTypes: Iterable<IrType>,
        outerClassTypeParameterScope: TypeParameterScope,
    ): ExportedDeclaration? {
        when (val exportability = klass.exportability()) {
            is Exportability.Prohibited -> irError(exportability.reason) {
                withIrEntry("klass", klass)
            }
            Exportability.NotNeeded -> return null
            Exportability.Implicit -> return exportDeclarationImplicitly(klass, superTypes, outerClassTypeParameterScope)
            Exportability.Allowed -> {}
        }

        val typeParameterScope = newTypeParameterScope(klass, outerClassTypeParameterScope, renameOuterTypeParameters = true)
        val (members, nestedClasses) = exportClassDeclarations(klass, superTypes, typeParameterScope)
        return exportClass(
            klass,
            superTypes,
            members,
            nestedClasses,
            typeParameterScope,
        )
    }

    private fun exportEnumClass(klass: IrClass, superTypes: Iterable<IrType>): ExportedDeclaration? {
        when (val exportability = klass.exportability()) {
            is Exportability.Prohibited -> irError(exportability.reason) {
                withIrEntry("klass", klass)
            }
            Exportability.NotNeeded -> return null
            Exportability.Implicit -> return exportDeclarationImplicitly(klass, superTypes, emptyMap())
            Exportability.Allowed -> {}
        }

        val enumEntries = klass
            .declarations
            .filterIsInstance<IrField>()
            .mapNotNull { it.correspondingEnumEntry }

        val enumEntriesToOrdinal: Map<IrEnumEntry, Int> =
            enumEntries
                .keysToMap(enumEntries::indexOf)

        val (members, nestedClasses) = exportClassDeclarations(klass, superTypes, emptyMap()) { candidate ->
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
            nestedClasses,
            emptyMap(),
        )
    }

    private fun exportClassDeclarations(
        klass: IrClass,
        superTypes: Iterable<IrType>,
        typeParameterScope: TypeParameterScope,
        specialProcessing: (IrDeclarationWithName) -> ExportedDeclaration? = { null }
    ): ExportedClassDeclarationsInfo {
        val members = mutableListOf<ExportedDeclaration>()
        val specialMembers = mutableListOf<ExportedDeclaration>()
        val nestedClasses = mutableListOf<ExportedClass>()
        val defaultImplementations = mutableListOf<ExportedDeclaration>()

        klass.forEachExportedMember(context) { candidate, declaration ->
            val processingResult = specialProcessing(candidate)
            if (processingResult != null) {
                specialMembers.add(processingResult)
                return@forEachExportedMember
            }

            when (candidate) {
                is IrSimpleFunction ->
                    exportFunction(candidate, typeParameterScope)
                        ?.withAttributesFor(candidate)
                        ?.let {
                            if (candidate.isExportedDefaultImplementation) {
                                defaultImplementations.add(it)
                            } else {
                                members.add(it)
                            }
                        }

                is IrConstructor ->
                    members.addIfNotNull(
                        exportConstructor(
                            candidate,
                            typeParameterScope,
                            isFactoryPropertyForInnerClass = false,
                        )?.withAttributesFor(candidate)
                    )

                is IrProperty ->
                    exportProperty(candidate, typeParameterScope)
                        ?.withAttributesFor(candidate)
                        ?.let {
                            if (candidate.isExportedDefaultImplementation) {
                                defaultImplementations.add(it)
                            } else {
                                members.add(it)
                            }
                        }

                is IrClass -> {
                    if (candidate.isInner && (candidate.modality == Modality.OPEN || candidate.modality == Modality.FINAL)) {
                        members.add(candidate.toFactoryPropertyForInnerClass(typeParameterScope).withAttributesFor(candidate))
                    }
                    val ec = exportClass(candidate, typeParameterScope)?.withAttributesFor(candidate)
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

        if (klass.shouldContainImplementableSymbolProperty()) {
            members.addOwnJsSymbolDeclaration()
            members.addImplementableSymbolProperty(klass)
        }

        if (!klass.isExternal) {
            members.addSuperTypesSpecialProperties(klass, superTypes, typeParameterScope)
        }

        if (defaultImplementations.isNotEmpty()) {
            members.add(
                ExportedNamespace(
                    StandardNames.DEFAULT_IMPLS_CLASS_NAME.identifier,
                    defaultImplementations,
                )
            )
        }

        return ExportedClassDeclarationsInfo(
            specialMembers + members,
            nestedClasses
        )
    }

    /**
     * Generates a property in the outer class that can be used to construct an instance of an inner class using Kotlin-like syntax.
     */
    private fun IrClass.toFactoryPropertyForInnerClass(outerClassTypeParameterScope: TypeParameterScope): ExportedProperty {
        val innerClassReference = typeScriptInnerClassReference()
        val typeMembers: List<ExportedDeclaration> = buildList {
            forEachExportedMember(context) { candidate, _ ->
                val exported = when (candidate) {
                    is IrConstructor -> {
                        val constructorTypeParameterScope =
                            newTypeParameterScope(this@toFactoryPropertyForInnerClass, outerClassTypeParameterScope)
                        exportConstructor(candidate, constructorTypeParameterScope, isFactoryPropertyForInnerClass = true)
                            ?.let { constructor ->
                                ExportedConstructSignature(
                                    parameters = constructor.parameters.drop(1),
                                    returnType = ExportedType.ClassType(
                                        name = innerClassReference,
                                        arguments = constructorTypeParameterScope.values.map(ExportedType::TypeParameterRef)
                                    ),
                                    typeParameters = typeParameters.map { constructorTypeParameterScope[it.symbol]!! },
                                    isProtected = constructor.isProtected,
                                )
                            }
                    }
                    is IrSimpleFunction if candidate.isStaticMethod -> exportFunction(
                        candidate,
                        outerClassTypeParameterScope,
                        isFactoryPropertyForInnerClass = true
                    )
                    is IrProperty if candidate.isStaticProperty -> exportProperty(
                        candidate,
                        outerClassTypeParameterScope,
                        isFactoryPropertyForInnerClass = true
                    )
                    else -> null
                }?.withAttributesFor(candidate)

                if (exported != null && !exported.isProtected) {
                    add(exported)
                }
            }
        }

        val name = getExportedIdentifier()

        return ExportedProperty(
            name = ExportedMemberName.Identifier(name),
            type = ExportedType.InlineInterfaceType(typeMembers),
            mutable = false,
            isMember = true,
        )
    }

    private fun IrValueParameter.shouldBeExported(): Boolean {
        return origin != JsLoweredDeclarationOrigin.JS_SUPER_CONTEXT_PARAMETER && origin != ES6_BOX_PARAMETER
    }

    private fun MutableList<ExportedDeclaration>.addOwnJsSymbolDeclaration() =
        add(
            ExportedProperty(
                name = ExportedMemberName.Identifier(ownImplementableSymbolName),
                type = ExportedType.Primitive.UniqueSymbol,
                mutable = false,
                isMember = false,
                isStatic = true,
                isField = true
            )
        )

    private fun MutableList<ExportedDeclaration>.addNotImplementableProperty(klass: IrClass) {
        add(
            ExportedProperty(
                name = ExportedMemberName.Identifier(notImplementablePropertyName),
                type = klass.generateNotImplementableBrandType(),
                mutable = false,
                isMember = true,
                isField = true
            )
        )
    }


    private fun MutableList<ExportedDeclaration>.addImplementableSymbolProperty(klass: IrClass) =
        add(
            ExportedProperty(
                name = ExportedMemberName.SymbolReference(
                    "${
                        klass.getFqNameWithJsNameWhenAvailable(
                            shouldIncludePackage = !isEsModules,
                            isEsModules = isEsModules
                        ).asString()
                    }.$ownImplementableSymbolName"
                ),
                type = ExportedType.LiteralType.BooleanLiteralType(true),
                mutable = false,
                isMember = true,
                isStatic = false,
                isField = true
            )
        )

    private fun MutableList<ExportedDeclaration>.addSuperTypesSpecialProperties(
        klass: IrClass,
        superTypes: Iterable<IrType>,
        typeParameterScope: TypeParameterScope,
    ) {
        val allSuperTypesWithBrandProperty = klass.collectAllImplementableAndNotImplementableInterfaces(superTypes)
        val typeItselfShouldNotBeImplemented = klass.shouldContainNotImplementableProperty()

        val (implementableSuperTypes, notImplementableSuperTypes) = allSuperTypesWithBrandProperty.partition { it is InterfaceSuperType.ImplementableInterface }

        implementableSuperTypes.forEach { superType ->
            addImplementableSymbolProperty(superType.irClass)
        }

        if (notImplementableSuperTypes.isEmpty()) {
            if (typeItselfShouldNotBeImplemented) addNotImplementableProperty(klass)
            return
        }

        val intersectionOfTypes = notImplementableSuperTypes
            .map { superType ->
                // TODO: rework it to stricter types instead of `any` for type parameters
                val mapping = superType.irClass.typeParameters.associate { it.symbol to context.dynamicType }
                ExportedType.PropertyType(
                    exportType(superType.irClass.defaultType.substitute(mapping), typeParameterScope),
                    ExportedType.LiteralType.StringLiteralType(notImplementablePropertyName),
                )
            }
            .reduce(ExportedType::IntersectionType)
            .let {
                if (typeItselfShouldNotBeImplemented) ExportedType.IntersectionType(
                    klass.generateNotImplementableBrandType(),
                    it
                ) else it
            }

        add(
            ExportedProperty(
                name = ExportedMemberName.Identifier(notImplementablePropertyName),
                type = intersectionOfTypes,
                mutable = false,
                isMember = true,
                isField = true
            )
        )
    }

    private fun IrClass.generateNotImplementableBrandType(): ExportedType {
        return ExportedType.InlineInterfaceType(
            listOf(
                ExportedProperty(
                    name = ExportedMemberName.Identifier(
                        getFqNameWithJsNameWhenAvailable(
                            shouldIncludePackage = true,
                            isEsModules = isEsModules
                        ).asString()
                    ),
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
        typeParameterScope: TypeParameterScope,
    ): ExportedDeclaration {
        val superClasses = superTypes
            .filter { !it.classifierOrFail.isInterface && it.canBeUsedAsSuperTypeOfExportedClasses() }
            .map { exportType(it, typeParameterScope, shouldCalculateExportedSupertypeForImplicit = false) }
            .memoryOptimizedFilter { it !is ExportedType.ErrorType }

        val superInterfaces = superTypes
            .filter { it.shouldPresentInsideImplementsClause() }
            .map { exportType(it, typeParameterScope, shouldCalculateExportedSupertypeForImplicit = false) }
            .memoryOptimizedFilter { it !is ExportedType.ErrorType }

        val name = klass.getExportedIdentifier()

        return if (klass.kind == ClassKind.OBJECT) {
            ExportedObject(
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
                typeParameters = typeParameterScope.values.toList(),
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
                                        name = it.getFqNameWithJsNameWhenAvailable(
                                            shouldIncludePackage = !isEsModules,
                                            isEsModules = isEsModules,
                                        ).asString(),
                                        arguments = emptyList()
                                    )
                                )
                            }
                    )
                    StandardNames.ENUM_VALUE_OF if enumEntriesToOrdinal.isEmpty() -> ExportedType.Primitive.Nothing
                    else -> null
                }
                exportFunction(candidate, emptyMap(), specializedType)
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
                        property = candidate,
                        classTypeParameterScope = emptyMap(),
                        specializeType = type,
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

    private fun exportTypeArgument(type: IrTypeArgument, typeOwner: IrDeclaration?, typeParameterScope: TypeParameterScope): ExportedType {
        if (type is IrTypeProjection)
            return exportType(type.type, typeParameterScope, typeOwner)

        return ExportedType.ErrorType("UnknownType ${type.render()}")
    }

    private typealias TypeParameterScope = Map<IrTypeParameterSymbol, ExportedTypeParameter>

    /**
     * We need to keep track of the type parameters that are currently in scope in order to correctly generate type parameters for inner
     * classes.
     *
     * In TypeScript, there is no notion of inner classes, so each type parameter that a Kotlin inner class captures from its outer class
     * (or classes) has to be declared explicitly in its TypeScript counterpart.
     *
     * For example, for the following Kotlin code:
     * ```kotlin
     * class Outer<T> {
     *   inner class Inner<S>
     * }
     * ```
     *
     * we need to generate the following TS code:
     * ```typescript
     * class Outer<T> {
     *   get Inner(): {
     *      new<S>(): Outer.Inner<S, T>;
     *   }
     * }
     * namespace Outer {
     *   class Inner<S, T$Outer> {
     *     // ...
     *   }
     * }
     * ```
     *
     * The explicit type parameters of the outer classes go immediately after the inner class's own type parameters.
     * The relative order of type parameters of each class is preserved.
     *
     * Note that we rename the captured type parameters by appending the names of its original parents separated with `$`.
     * This is done for clarity and to avoid name clashes.
     */
    private fun newTypeParameterScope(
        container: IrTypeParametersContainer,
        outerScope: TypeParameterScope,
        renameOuterTypeParameters: Boolean = false,
    ): TypeParameterScope = buildMap {
        val newTypeParameters = container.typeParameters
        val shouldIncludeOuterScope = container !is IrClass || container.isInner

        val nameTable = NameTable<IrTypeParameterSymbol>()
        if (shouldIncludeOuterScope && !renameOuterTypeParameters) {
            for ((irTypeParameter, exported) in outerScope) {
                nameTable.declareStableName(irTypeParameter, exported.name)
            }
        }

        // First, create all the exported type parameters without constraints, because constraints may reference a type parameter
        // that we haven't yet met.
        for (tp in newTypeParameters) {
            this[tp.symbol] = ExportedTypeParameter(nameTable.declareFreshName(tp.symbol, tp.name.identifier))
        }

        var shouldRecomputeOuterConstraints = false
        if (shouldIncludeOuterScope) {
            if (renameOuterTypeParameters) {
                for ((irTypeParameter, exported) in outerScope) {
                    shouldRecomputeOuterConstraints = true
                    val disambiguatedName = irTypeParameter.owner.parentDeclarationsWithSelf.joinToString(separator = "\$") {
                        (it as IrDeclarationWithName).getExportedIdentifier()
                    }
                    this[irTypeParameter] = exported.copy(name = nameTable.declareFreshName(irTypeParameter, disambiguatedName))
                }
            } else {
                putAll(outerScope)
            }
        }

        // Then compute the constraints
        var i = 0
        for ((tp, exported) in this) {
            if (!shouldRecomputeOuterConstraints && i == newTypeParameters.size) {
                // Don't compute constraints for type parameters from the `outerScope` map, they should already be computed at this point.
                // Unless we've renamed those type parameters, in which case we have to compute the constraints for them again.
                break
            }
            i += 1
            val constraints = tp.owner.superTypes.asSequence()
                .filter { it != context.irBuiltIns.anyNType }
                .map {
                    val exportedType = exportType(it, this)
                    if (exportedType is ExportedType.ImplicitlyExportedType && exportedType.exportedSupertype == ExportedType.Primitive.Any) {
                        exportedType.copy(exportedSupertype = ExportedType.Primitive.Unknown)
                    } else {
                        exportedType
                    }
                }
                .filter { it !is ExportedType.ErrorType }
                .toList()

            exported.constraint = constraints.run {
                when (size) {
                    0 -> null
                    1 -> single()
                    else -> reduce(ExportedType::IntersectionType)
                }
            }
        }
    }

    private val currentlyProcessedTypes = hashSetOf<IrType>()

    private fun exportType(
        type: IrType,
        typeParameterScope: TypeParameterScope,
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
            nonNullType.isArray() -> ExportedType.Array(exportTypeArgument(nonNullType.arguments[0], typeOwner, typeParameterScope))
            nonNullType.isSuspendFunction() -> ExportedType.ErrorType("Suspend functions are not supported")
            nonNullType.isFunction() -> ExportedType.Function(
                parameters = nonNullType.arguments.dropLast(1).memoryOptimizedMap {
                    ExportedParameter(
                        name = (it as? IrTypeProjection)?.type?.getAnnotationArgumentValue(StandardNames.FqNames.parameterName, "name"),
                        type = exportTypeArgument(it, typeOwner, typeParameterScope),
                    )
                },
                returnType = exportTypeArgument(nonNullType.arguments.last(), typeOwner, typeParameterScope)
            )

            classifier is IrTypeParameterSymbol -> typeParameterScope[classifier]?.let(ExportedType::TypeParameterRef)
                ?: error("Type parameter '${classifier.owner.render()}' is not in scope")

            classifier is IrClassSymbol -> {
                val klass = classifier.owner
                val isExported = klass.isExportedImplicitlyOrExplicitly(context)
                val isImplicitlyExported = !isExported && !klass.isExternal
                val isNonExportedExternal = klass.isExternal && !isExported
                val name = klass.getFqNameWithJsNameWhenAvailable(
                    shouldIncludePackage = !isNonExportedExternal && !isEsModules,
                    isEsModules = isEsModules,
                ).asString()

                val exportedSupertype = runIf(shouldCalculateExportedSupertypeForImplicit && isImplicitlyExported) {
                    val transitiveExportedType = nonNullType.collectSuperTransitiveHierarchy()
                    if (transitiveExportedType.isEmpty()) return@runIf null
                    transitiveExportedType
                        .memoryOptimizedMap { exportType(it, typeParameterScope, typeOwner) }
                        .reduce(ExportedType::IntersectionType)
                } ?: ExportedType.Primitive.Any

                val classType = ExportedType.ClassType(
                    name = name,
                    arguments = type.arguments.memoryOptimizedMap { exportTypeArgument(it, typeOwner, typeParameterScope) },
                    classId = klass.classId,
                )

                when (klass.kind) {
                    ClassKind.ANNOTATION_CLASS,
                    ClassKind.ENUM_ENTRY,
                        -> ExportedType.ErrorType("Class $name with kind: ${klass.kind}")

                    ClassKind.OBJECT -> ExportedType.TypeOf(classType)

                    ClassKind.CLASS,
                    ClassKind.ENUM_CLASS,
                    ClassKind.INTERFACE,
                        -> classType
                }.withImplicitlyExported(isImplicitlyExported, exportedSupertype)
            }

            else -> irError("Unexpected classifier") {
                withIrEntry("classifier.owner", classifier.owner)
            }
        }

        return exportedType.withNullability(isMarkedNullable)
            .also { currentlyProcessedTypes.remove(type) }
    }


    /**
     * With this method we're collecting all the super types that may contain either implementable or non-implementable properties
     * - For interfaces we're looking only parents that contain not-implementable properties
     * - For classes we're looking for both kinds of super-types
     *
     * We're collecting such information to copy those properties into the current declaration to generate a valid TypeScript definition
     *
     * As an example:
     * ```kotlin
     * @JsExport interface Foo
     * @JsExport interface Bar
     *
     * class NotExportedParent : Foo, Bar
     *
     * @JsExport
     * class ExportedChild : NotExportedParent
     * ```
     *
     * For such a class we should do the following:
     * 1. Implementable interfaces and no strict implicit export
     * ```typescript
     * declare interface Foo { readonly [Foo.Symbol]: true }
     * declare namespace Foo { const Symbol: unique symbol; }
     *
     * declare interface Bar { readonly [Bar.Symbol]: true }
     * declare namespace Bar { const Symbol: unique symbol; }
     *
     * declare  class ExportedChild implements Foo, Bar {
     *   readonly [Foo.Symbol]: true;
     *   readonly [Bar.Symbol]: true;
     * }
     * ```
     * 2. Implementable interfaces and strict implicit export
     * ```typescript
     * declare interface Foo { readonly [Foo.Symbol]: true }
     * declare namespace Foo { const Symbol: unique symbol; }
     *
     * declare interface Bar { readonly [Bar.Symbol]: true }
     * declare namespace Bar { const Symbol: unique symbol; }
     *
     * declare interface NotExportedParent extends Foo, Bar {
     *    readonly __doNotUseOrImplementIt: {
     *      readonly "NotExportedParent": unique symbol;
     *    };
     * }
     *
     * declare class ExportedChild implements NotExportedParent {
     *   readonly [Foo.Symbol]: true;
     *   readonly [Bar.Symbol]: true;
     *   readonly __doNotUseOrImplementIt: {
     *      readonly "NotExportedParent": unique symbol;
     *   };
     * }
     * ```
     *
     * 3. Not-implementable interfaces and no strict implicit export
     * ```typescript
     * declare interface Foo {
     *    readonly __doNotUseOrImplementIt: {
     *      readonly "Foo": unique symbol;
     *    };
     * }
     *
     * declare interface Bar {
     *    readonly __doNotUseOrImplementIt: {
     *      readonly "Bar": unique symbol;
     *    };
     * }
     *
     * declare class ExportedChild implements Foo, Bar {
     *   readonly __doNotUseOrImplementIt: Foo["__doNotUseOrImplementIt"] & Bar["__doNotUseOrImplementIt"]
     * }
     * ```
     *
     * 3. Not-implementable interfaces and strict implicit export
     * ```typescript
     * declare interface Foo {
     *    readonly __doNotUseOrImplementIt: {
     *      readonly "Foo": unique symbol;
     *    };
     * }
     *
     * declare interface Bar {
     *    readonly __doNotUseOrImplementIt: {
     *      readonly "Bar": unique symbol;
     *    };
     * }
     *
     * declare interface NotExportedParent extends Foo, Bar {
     *    readonly __doNotUseOrImplementIt: Foo["__doNotUseOrImplementIt"] & Bar["__doNotUseOrImplementIt"] & {
     *      readonly "NotExportedParent": unique symbol;
     *    };
     * }
     *
     * declare class ExportedChild implements NotExportedParent {
     *   readonly __doNotUseOrImplementIt: NotExportedParent["__doNotUseOrImplementIt"]
     * }
     * ```
     *
     * Because of such complications, I believe we should remove the strict-implicit export in future (since it's unstable and we don't have a plan to support it in future)
     */
    // TODO: think about per class memoization
    private fun IrClass.collectAllImplementableAndNotImplementableInterfaces(superTypes: Iterable<IrType>): Collection<InterfaceSuperType> {
        fun MutableCollection<IrClass>.enqueueSuperTypes(superTypes: Iterable<IrType>) =
            superTypes.mapNotNullTo(this) { superType ->
                superType.type.classOrNull?.owner?.takeIf { !it.isExternal }
            }

        val shouldCopySymbolsOfTransitiveParents = allowImplementingInterfaces && !isInterface

        // If we're processing an interface:
        // - If it's not implementable, we just need to add its direct not-implementable super types to generate a correct type for __doNotUseOrImplementIt
        // - If it's implementable, we don't need to generate anything, since it's already receiving all the properties through the inheritance
        // If we're processing a class, we should collect:
        // - All the implementable interfaces in the hierarchy to add the correct Symbol
        // - All the nearest not-implementable interfaces to generate a correct type for __doNotUseOrImplementIt
        val result = linkedMapOf<IrClass, InterfaceSuperType>()
        val stack = mutableListOf<IrClass>()
            .apply { enqueueSuperTypes(superTypes) }

        while (stack.isNotEmpty()) {
            val processedClass = stack.removeLast().takeIf { it !in result } ?: continue

            if (processedClass.isJsImplicitExport()) {
                result[processedClass] = InterfaceSuperType.NotImplementableInterface(processedClass)
                continue
            }

            if (!processedClass.isExported(context) || processedClass.isExternal) continue

            if (processedClass.isInterface && !processedClass.isJsNoRuntime()) {
                if (allowImplementingInterfaces) {
                    if (!shouldCopySymbolsOfTransitiveParents) continue
                    result[processedClass] = InterfaceSuperType.ImplementableInterface(processedClass)
                } else {
                    result[processedClass] = InterfaceSuperType.NotImplementableInterface(processedClass)
                    continue
                }
            }

            if (result.isNotEmpty()) {
                stack.enqueueSuperTypes(processedClass.superTypes)
            }
        }

        return result.values
    }
}

private sealed interface InterfaceSuperType {
    data class ImplementableInterface(override val irClass: IrClass) : InterfaceSuperType
    data class NotImplementableInterface(override val irClass: IrClass) : InterfaceSuperType

    val irClass: IrClass
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

private val IrProperty.isStaticProperty: Boolean
    get() = (getter ?: setter)?.isStaticMethodOfClass == true

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
