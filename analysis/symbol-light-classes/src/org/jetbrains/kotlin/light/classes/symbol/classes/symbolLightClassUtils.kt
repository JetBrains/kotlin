/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiReferenceList
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.platform.modification.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassErrorType
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.hasInterfaceDefaultImpls
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.*
import org.jetbrains.kotlin.light.classes.symbol.copy
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty
import org.jetbrains.kotlin.light.classes.symbol.isJvmField
import org.jetbrains.kotlin.light.classes.symbol.mapType
import org.jetbrains.kotlin.light.classes.symbol.methods.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import java.util.*

internal fun createSymbolLightClassNoCache(classOrObject: KtClassOrObject, ktModule: KaModule): KtLightClass? = when {
    classOrObject.isObjectLiteral() -> SymbolLightClassForAnonymousObject(classOrObject, ktModule)
    classOrObject is KtEnumEntry -> lightClassForEnumEntry(classOrObject)
    else -> createLightClassNoCache(classOrObject, ktModule)
}

internal fun createLightClassNoCache(ktClassOrObject: KtClassOrObject, ktModule: KaModule): SymbolLightClassBase = when {
    ktClassOrObject.hasModifier(INLINE_KEYWORD) || ktClassOrObject.hasModifier(VALUE_KEYWORD) ->
        SymbolLightClassForValueClass(ktClassOrObject, ktModule)

    ktClassOrObject is KtClass && ktClassOrObject.isAnnotation() -> SymbolLightClassForAnnotationClass(ktClassOrObject, ktModule)
    ktClassOrObject is KtClass && ktClassOrObject.isInterface() -> SymbolLightClassForInterface(ktClassOrObject, ktModule)
    else -> SymbolLightClassForClassOrObject(ktClassOrObject, ktModule)
}

internal fun KtClassOrObject.contentModificationTrackers(): List<ModificationTracker> {
    val outOfBlockTracker = project.createProjectWideOutOfBlockModificationTracker()
    return if (isLocal) {
        val file = containingKtFile
        listOf(outOfBlockTracker, ModificationTracker { file.modificationStamp })
    } else {
        listOf(outOfBlockTracker)
    }
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun createLightClassNoCache(
    classSymbol: KaNamedClassSymbol,
    ktModule: KaModule,
    manager: PsiManager,
): SymbolLightClassBase = when (classSymbol.classKind) {
    KaClassKind.INTERFACE -> SymbolLightClassForInterface(
        ktAnalysisSession = this@KaSession,
        ktModule = ktModule,
        classSymbol = classSymbol,
        manager = manager,
    )

    KaClassKind.ANNOTATION_CLASS -> SymbolLightClassForAnnotationClass(
        ktAnalysisSession = this@KaSession,
        ktModule = ktModule,
        classSymbol = classSymbol,
        manager = manager,
    )

    else -> if (classSymbol.isInline) {
        SymbolLightClassForValueClass(
            ktAnalysisSession = this@KaSession,
            ktModule = ktModule,
            classSymbol = classSymbol,
            manager = manager,
        )
    } else {
        SymbolLightClassForClassOrObject(
            ktAnalysisSession = this@KaSession,
            ktModule = ktModule,
            classSymbol = classSymbol,
            manager = manager,
        )
    }
}

private fun lightClassForEnumEntry(ktEnumEntry: KtEnumEntry): KtLightClass? {
    if (ktEnumEntry.body == null) return null

    val symbolLightClass = ktEnumEntry.containingClass()?.toLightClass() as? SymbolLightClassForClassOrObject ?: return null
    val targetField = symbolLightClass.ownFields.firstOrNull {
        it is SymbolLightFieldForEnumEntry && it.kotlinOrigin == ktEnumEntry
    } ?: return null

    return (targetField as? SymbolLightFieldForEnumEntry)?.initializingClass as? KtLightClass
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun SymbolLightClassBase.createConstructors(
    declarations: Sequence<KaConstructorSymbol>,
    result: MutableList<PsiMethod>,
) {
    val constructors = declarations.toList()
    if (constructors.isEmpty()) {
        result.add(defaultConstructor())
        return
    }

    for (constructor in constructors) {
        ProgressManager.checkCanceled()

        if (constructor.isHiddenOrSynthetic()) continue

        result.add(
            SymbolLightConstructor(
                ktAnalysisSession = this@KaSession,
                constructorSymbol = constructor,
                containingClass = this@createConstructors,
                methodIndex = METHOD_INDEX_BASE
            )
        )

        createJvmOverloadsIfNeeded(constructor, result) { methodIndex, argumentSkipMask ->
            SymbolLightConstructor(
                ktAnalysisSession = this@KaSession,
                constructorSymbol = constructor,
                containingClass = this@createConstructors,
                methodIndex = methodIndex,
                argumentsSkipMask = argumentSkipMask
            )
        }
    }
    val primaryConstructor = constructors.singleOrNull { it.isPrimary }
    if (primaryConstructor != null && shouldGenerateNoArgOverload(primaryConstructor, constructors)) {
        result.add(
            noArgConstructor(
                primaryConstructor.compilerVisibility.externalDisplayName,
                primaryConstructor.sourcePsiSafe(),
                METHOD_INDEX_FOR_NO_ARG_OVERLOAD_CTOR
            )
        )
    }
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun SymbolLightClassBase.shouldGenerateNoArgOverload(
    primaryConstructor: KaConstructorSymbol,
    constructors: Iterable<KaConstructorSymbol>,
): Boolean {
    val classOrObject = kotlinOrigin ?: return false
    return primaryConstructor.visibility != KaSymbolVisibility.PRIVATE &&
            !classOrObject.hasModifier(INNER_KEYWORD) && !isEnum &&
            !classOrObject.hasModifier(SEALED_KEYWORD) &&
            primaryConstructor.valueParameters.isNotEmpty() &&
            primaryConstructor.valueParameters.all { it.hasDefaultValue } &&
            constructors.none { it.valueParameters.isEmpty() } &&
            !primaryConstructor.hasJvmOverloadsAnnotation()
}

private fun SymbolLightClassBase.defaultConstructor(): KtLightMethod {
    val classOrObject = kotlinOrigin
    val visibility = when {
        this is SymbolLightClassForClassLike<*> && (classKind().let { it.isObject || it == KaClassKind.ENUM_CLASS }) -> PsiModifier.PRIVATE
        classOrObject?.hasModifier(SEALED_KEYWORD) == true -> PsiModifier.PROTECTED
        this is SymbolLightClassForEnumEntry -> PsiModifier.PACKAGE_LOCAL
        else -> PsiModifier.PUBLIC
    }

    return noArgConstructor(visibility, classOrObject, METHOD_INDEX_FOR_DEFAULT_CTOR)
}

private fun SymbolLightClassBase.noArgConstructor(
    visibility: String,
    declaration: KtDeclaration?,
    methodIndex: Int,
): KtLightMethod = SymbolLightNoArgConstructor(
    declaration?.let {
        LightMemberOriginForDeclaration(
            originalElement = it,
            originKind = JvmDeclarationOriginKind.OTHER,
        )
    },
    this,
    visibility,
    methodIndex,
)

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun SymbolLightClassBase.createMethods(
    declarations: Sequence<KaCallableSymbol>,
    result: MutableList<PsiMethod>,
    isTopLevel: Boolean = false,
    suppressStatic: Boolean = false
) {
    val (ctorProperties, regularMembers) = declarations.partition { it is KaPropertySymbol && it.isFromPrimaryConstructor }

    fun KaSession.handleDeclaration(declaration: KaCallableSymbol) {
        when (declaration) {
            is KaNamedFunctionSymbol -> {
                ProgressManager.checkCanceled()

                if (declaration.hasReifiedParameters || declaration.isHiddenOrSynthetic()) return
                if (declaration.name.isSpecial) return

                result.add(
                    SymbolLightSimpleMethod(
                        ktAnalysisSession = this,
                        functionSymbol = declaration,
                        lightMemberOrigin = null,
                        containingClass = this@createMethods,
                        methodIndex = METHOD_INDEX_BASE,
                        isTopLevel = isTopLevel,
                        suppressStatic = suppressStatic
                    )
                )

                createJvmOverloadsIfNeeded(declaration, result) { methodIndex, argumentSkipMask ->
                    SymbolLightSimpleMethod(
                        ktAnalysisSession = this,
                        functionSymbol = declaration,
                        lightMemberOrigin = null,
                        containingClass = this@createMethods,
                        methodIndex = methodIndex,
                        isTopLevel = isTopLevel,
                        argumentsSkipMask = argumentSkipMask,
                        suppressStatic = suppressStatic
                    )
                }
            }

            is KaPropertySymbol -> createPropertyAccessors(
                result,
                declaration,
                isTopLevel = isTopLevel,
                suppressStatic = suppressStatic
            )

            is KaConstructorSymbol -> error("Constructors should be handled separately and not passed to this function")
            else -> {}
        }
    }

    // Regular members
    regularMembers.forEach {
        this@KaSession.handleDeclaration(it)
    }
    // Then, properties from the primary constructor parameters
    ctorProperties.forEach {
        this@KaSession.handleDeclaration(it)
    }
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private inline fun <T : KaFunctionSymbol> createJvmOverloadsIfNeeded(
    declaration: T,
    result: MutableList<PsiMethod>,
    lightMethodCreator: (Int, BitSet) -> PsiMethod
) {
    if (!declaration.hasJvmOverloadsAnnotation()) return
    var methodIndex = METHOD_INDEX_BASE
    val skipMask = BitSet(declaration.valueParameters.size)
    for (i in declaration.valueParameters.size - 1 downTo 0) {
        if (!declaration.valueParameters[i].hasDefaultValue) continue
        skipMask.set(i)
        result.add(
            lightMethodCreator.invoke(methodIndex++, skipMask.copy())
        )
    }
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun SymbolLightClassBase.createPropertyAccessors(
    result: MutableList<PsiMethod>,
    declaration: KaPropertySymbol,
    isTopLevel: Boolean,
    isMutable: Boolean = !declaration.isVal,
    onlyJvmStatic: Boolean = false,
    suppressStatic: Boolean = false,
) {
    ProgressManager.checkCanceled()

    if (declaration.name.isSpecial) return

    val originalElement = declaration.sourcePsiSafe<KtDeclaration>()

    val generatePropertyAnnotationsMethods =
        (declaration.containingModule as? KaSourceModule)
            ?.languageVersionSettings
            ?.getFlag(JvmAnalysisFlags.generatePropertyAnnotationsMethods) == true

    if (generatePropertyAnnotationsMethods && !this@createPropertyAccessors.isAnnotationType && declaration.psi?.parentOfType<KtClassOrObject>() == this.kotlinOrigin) {
        val lightMemberOrigin = originalElement?.let {
            LightMemberOriginForDeclaration(
                originalElement = it,
                originKind = JvmDeclarationOriginKind.OTHER,
            )
        }
        val method = SymbolLightAnnotationsMethod(
            ktAnalysisSession = this@KaSession,
            containingPropertySymbol = declaration,
            lightMemberOrigin = lightMemberOrigin,
            containingClass = this@createPropertyAccessors
        )
        if (method.annotations.size > 1) { // There's always a @java.lang.Deprecated
            result.add(method)
        }
    }

    if (declaration is KaKotlinPropertySymbol && declaration.isConst) return
    if (declaration.getter?.hasBody != true && declaration.setter?.hasBody != true && declaration.visibility == KaSymbolVisibility.PRIVATE) return

    if (declaration.isJvmField) return
    val propertyTypeIsValueClass = declaration.hasTypeForValueClassInSignature(suppressJvmNameCheck = true)

    fun KaPropertyAccessorSymbol.needToCreateAccessor(siteTarget: AnnotationUseSiteTarget): Boolean {
        when {
            !propertyTypeIsValueClass -> {}
            /*
             * For top-level properties with value class in return type compiler mangles only setter
             *
             *   @JvmInline
             *   value class Some(val value: String)
             *
             *   var topLevelProp: Some = Some("1")
             *
             * Compiles to
             *   public final class FooKt {
             *     public final static getTopLevelProp()Ljava/lang/String;
             *
             *     public final static setTopLevelProp-5lyY9Q4(Ljava/lang/String;)V
             *
             *     private static Ljava/lang/String; topLevelProp
             *  }
             */
            this is KaPropertyGetterSymbol && this@createPropertyAccessors is SymbolLightClassForFacade -> {}
            // Accessors with JvmName can be accessible from Java
            hasJvmNameAnnotation() -> {}
            else -> return false
        }

        if (onlyJvmStatic && !hasJvmStaticAnnotation() && !declaration.hasJvmStaticAnnotation()) return false

        if (declaration.hasReifiedParameters) return false
        if (isHiddenByDeprecation(declaration)) return false
        if (isHiddenOrSynthetic(siteTarget)) return false
        if (!hasBody && visibility == KaSymbolVisibility.PRIVATE) return false

        return true
    }

    val getter = declaration.getter?.takeIf {
        it.needToCreateAccessor(AnnotationUseSiteTarget.PROPERTY_GETTER)
    }

    fun createSymbolLightAccessorMethod(accessor: KaPropertyAccessorSymbol): SymbolLightAccessorMethod {
        // [KtFakeSourceElementKind.DelegatedPropertyAccessor] is not allowed as source PSI, e.g.,
        //
        //   val p by delegate(...)
        //
        // However, we also lose the source PSI of a custom property accessor, e.g.,
        //
        //   val p by delegate(...)
        //     get() = ...
        //
        // We go upward to the property's source PSI and attempt to find/bind accessor's source PSI.
        fun sourcePsiFromProperty(): KtPropertyAccessor? {
            if (accessor.origin != KaSymbolOrigin.SOURCE) return null
            val propertyPsi = declaration.psi as? KtProperty ?: return null
            return if (accessor is KaPropertyGetterSymbol)
                propertyPsi.getter
            else
                propertyPsi.setter
        }

        val lightMemberOrigin = originalElement?.let {
            LightMemberOriginForDeclaration(
                originalElement = it,
                originKind = JvmDeclarationOriginKind.OTHER,
                auxiliaryOriginalElement = accessor.sourcePsiSafe<KtDeclaration>() ?: sourcePsiFromProperty()
            )
        }

        return SymbolLightAccessorMethod(
            ktAnalysisSession = this@KaSession,
            propertyAccessorSymbol = accessor,
            containingPropertySymbol = declaration,
            lightMemberOrigin = lightMemberOrigin,
            containingClass = this@createPropertyAccessors,
            isTopLevel = isTopLevel,
            suppressStatic = suppressStatic,
        )
    }

    if (getter != null) {
        result.add(createSymbolLightAccessorMethod(getter))
    }

    val setter = declaration.setter?.takeIf {
        !isAnnotationType && it.needToCreateAccessor(AnnotationUseSiteTarget.PROPERTY_SETTER)
    }

    if (isMutable && setter != null) {
        result.add(createSymbolLightAccessorMethod(setter))
    }
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun SymbolLightClassBase.createAndAddField(
    declaration: KaPropertySymbol,
    nameGenerator: SymbolLightField.FieldNameGenerator,
    isStatic: Boolean,
    result: MutableList<PsiField>
) {
    val field = createField(declaration, nameGenerator, isStatic) ?: return
    result += field
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun SymbolLightClassBase.createField(
    declaration: KaPropertySymbol,
    nameGenerator: SymbolLightField.FieldNameGenerator,
    isStatic: Boolean,
): SymbolLightFieldForProperty? {
    ProgressManager.checkCanceled()

    if (declaration.name.isSpecial) return null
    if (!hasBackingField(declaration)) return null

    val isDelegated = (declaration as? KaKotlinPropertySymbol)?.isDelegatedProperty == true
    val fieldName = nameGenerator.generateUniqueFieldName(
        declaration.name.asString() + (if (isDelegated) JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX else "")
    )

    return SymbolLightFieldForProperty(
        ktAnalysisSession = this@KaSession,
        propertySymbol = declaration,
        fieldName = fieldName,
        containingClass = this,
        lightMemberOrigin = null,
        isStatic = isStatic,
    )
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun hasBackingField(property: KaPropertySymbol): Boolean {
    if (property is KaSyntheticJavaPropertySymbol) return true
    requireIsInstance<KaKotlinPropertySymbol>(property)

    if (property.origin.cannotHasBackingField() || property.isStatic) return false
    if (property.isLateInit || property.isDelegatedProperty || property.isFromPrimaryConstructor) return true
    val hasBackingFieldByPsi: Boolean? = property.psi?.hasBackingField()
    if (hasBackingFieldByPsi == false) {
        return hasBackingFieldByPsi
    }

    if (property.isExpect ||
        property.modality == KaSymbolModality.ABSTRACT ||
        property.backingFieldSymbol?.hasJvmSyntheticAnnotation() == true
    ) return false

    return hasBackingFieldByPsi ?: property.hasBackingField
}

private fun KaSymbolOrigin.cannotHasBackingField(): Boolean =
    this == KaSymbolOrigin.SOURCE_MEMBER_GENERATED ||
            this == KaSymbolOrigin.DELEGATED ||
            this == KaSymbolOrigin.INTERSECTION_OVERRIDE ||
            this == KaSymbolOrigin.SUBSTITUTION_OVERRIDE

private fun PsiElement.hasBackingField(): Boolean {
    if (this is KtParameter) return true
    if (this !is KtProperty) return false

    return hasInitializer() || getter?.takeIf { it.hasBody() } == null || setter?.takeIf { it.hasBody() } == null && isVar
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun SymbolLightClassForClassLike<*>.createInheritanceList(
    forExtendsList: Boolean,
    superTypes: List<KaType>,
): PsiReferenceList {
    val role = if (forExtendsList) PsiReferenceList.Role.EXTENDS_LIST else PsiReferenceList.Role.IMPLEMENTS_LIST

    val listBuilder = KotlinSuperTypeListBuilder(
        this,
        kotlinOrigin = kotlinOrigin?.getSuperTypeList(),
        manager = manager,
        language = language,
        role = role,
    )

    fun KaType.needToAddTypeIntoList(): Boolean {
        // Do not add redundant "extends java.lang.Object" anywhere
        if (this.isAnyType) return false
        // Interfaces have only extends lists
        if (isInterface) return forExtendsList

        return when (this) {
            is KaClassType -> {
                // We don't have Enum among enums supertype in sources neither we do for decompiled class-files and light-classes
                if (isEnum && this.classId == StandardClassIds.Enum) return false

                // NB: need to expand type alias, e.g., kotlin.Comparator<T> -> java.util.Comparator<T>
                val classKind = expandedSymbol?.classKind
                val isJvmInterface = classKind == KaClassKind.INTERFACE || classKind == KaClassKind.ANNOTATION_CLASS

                forExtendsList == !isJvmInterface
            }

            is KaClassErrorType -> {
                val superList = this@createInheritanceList.kotlinOrigin?.getSuperTypeList() ?: return false
                val qualifierName = this.qualifiers.joinToString(".") { it.name.asString() }.takeIf { it.isNotEmpty() } ?: return false
                val isConstructorCall = superList.findEntry(qualifierName) is KtSuperTypeCallEntry

                forExtendsList == isConstructorCall
            }

            else -> false
        }
    }

    superTypes.asSequence()
        .filter { it.needToAddTypeIntoList() }
        .forEach { superType ->
            val mappedType = mapType(
                superType,
                this@createInheritanceList,
                KaTypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS
            ) ?: return@forEach
            listBuilder.addReference(mappedType)
            if (mappedType.canonicalText.startsWith("kotlin.collections.")) {
                val mappedToNoCollectionAsIs = mapType(superType, this@createInheritanceList, KaTypeMappingMode.SUPER_TYPE)
                if (mappedToNoCollectionAsIs != null &&
                    mappedType.canonicalText != mappedToNoCollectionAsIs.canonicalText
                ) {
                    // Add java supertype
                    listBuilder.addReference(mappedToNoCollectionAsIs)
                    // Add marker interface
                    if (superType is KaClassType) {
                        listBuilder.addMarkerInterfaceIfNeeded(superType.classId)
                    }
                }
            }
        }

    return listBuilder
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun KaDeclarationContainerSymbol.createInnerClasses(
    manager: PsiManager,
    containingClass: SymbolLightClassBase,
    classOrObject: KtClassOrObject?
): List<SymbolLightClassBase> {
    val result = SmartList<SymbolLightClassBase>()

    staticDeclaredMemberScope.classifiers.filterIsInstance<KaNamedClassSymbol>().mapNotNullTo(result) {
        val classOrObjectDeclaration = it.sourcePsiSafe<KtClassOrObject>()
        if (classOrObjectDeclaration != null) {
            classOrObjectDeclaration.toLightClass() as? SymbolLightClassBase
        } else {
            createLightClassNoCache(it, ktModule = containingClass.ktModule, manager)
        }
    }

    val jvmDefaultMode = classOrObject
        ?.let { getModule(it) as? KaSourceModule }
        ?.languageVersionSettings
        ?.getFlag(JvmAnalysisFlags.jvmDefaultMode)
        ?: JvmDefaultMode.DISABLE

    if (containingClass is SymbolLightClassForInterface &&
        classOrObject?.hasInterfaceDefaultImpls == true &&
        jvmDefaultMode != JvmDefaultMode.ALL
    ) {
        result.add(SymbolLightClassForInterfaceDefaultImpls(containingClass))
    }

    if (containingClass is SymbolLightClassForAnnotationClass &&
        this is KaNamedClassSymbol &&
        StandardClassIds.Annotations.Repeatable in annotations &&
        JvmStandardClassIds.Annotations.Java.Repeatable !in annotations
    ) {
        result.add(SymbolLightClassForRepeatableAnnotationContainer(containingClass))
    }

    return result
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun KtClassOrObject.checkIsInheritor(superClassOrigin: KtClassOrObject, checkDeep: Boolean): Boolean {
    if (this == superClassOrigin) return false
    if (superClassOrigin is KtEnumEntry) {
        return false // enum entry cannot have inheritors
    }
    if (!superClassOrigin.canBeAnalysed()) {
        return false
    }

    val superClassSymbol = superClassOrigin.classSymbol ?: return false

    when (this) {
        is KtEnumEntry -> {
            val enumEntrySymbol = this.symbol
            val classId = enumEntrySymbol.callableId?.classId ?: return false
            val enumClassSymbol = findClass(classId) ?: return false
            if (enumClassSymbol == superClassSymbol) return true
            return if (checkDeep) {
                enumClassSymbol.isSubClassOf(superClassSymbol)
            } else {
                false
            }
        }

        else -> {
            val subClassSymbol = this.classSymbol

            if (subClassSymbol == null || subClassSymbol == superClassSymbol) return false

            return if (checkDeep) {
                subClassSymbol.isSubClassOf(superClassSymbol)
            } else {
                subClassSymbol.isDirectSubClassOf(superClassSymbol)
            }
        }
    }
}

private val KaDeclarationSymbol.hasReifiedParameters: Boolean
    get() = typeParameters.any { it.isReified }

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun SymbolLightClassBase.addPropertyBackingFields(
    result: MutableList<PsiField>,
    containerSymbol: KaDeclarationContainerSymbol,
    nameGenerator: SymbolLightField.FieldNameGenerator,
    forceIsStaticTo: Boolean? = null,
) {
    val propertySymbols = containerSymbol.combinedDeclaredMemberScope.callables
        .filterIsInstance<KaPropertySymbol>()
        .applyIf(containerSymbol is KaClassSymbol && containerSymbol.classKind == KaClassKind.COMPANION_OBJECT) {
            // All fields for companion object of classes are generated to the containing class
            // For interfaces, only @JvmField-annotated properties are generated to the containing class
            // Probably, the same should work for const vals but it doesn't at the moment (see KT-28294)
            filter { containingClass?.isInterface == true && !it.isJvmField }
        }

    val (ctorProperties, memberProperties) = propertySymbols.partition { it.isFromPrimaryConstructor }
    val isStatic = forceIsStaticTo ?: (containerSymbol is KaClassSymbol && containerSymbol.classKind.isObject)
    fun addPropertyBackingField(propertySymbol: KaPropertySymbol) {
        createAndAddField(
            declaration = propertySymbol,
            nameGenerator = nameGenerator,
            isStatic = isStatic,
            result = result
        )
    }

    // First, properties from parameters
    ctorProperties.forEach(::addPropertyBackingField)
    // Then, regular member properties
    memberProperties.forEach(::addPropertyBackingField)
}

/**
 * @param suppressJvmNameCheck **true** if [hasJvmNameAnnotation] should be omitted.
 * E.g., if [JvmName] is checked manually later
 */
context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun KaCallableSymbol.hasTypeForValueClassInSignature(
    ignoreReturnType: Boolean = false,
    suppressJvmNameCheck: Boolean = false,
): Boolean {
    // Declarations with JvmName can be accessible from Java
    when {
        suppressJvmNameCheck -> {}
        hasJvmNameAnnotation() -> return false
        this !is KaKotlinPropertySymbol -> {}
        getter?.hasJvmNameAnnotation() == true || setter?.hasJvmNameAnnotation() == true -> return false
    }

    if (!ignoreReturnType) {
        val psiDeclaration = sourcePsiSafe<KtCallableDeclaration>()
        if (psiDeclaration?.typeReference != null && returnType.typeForValueClass) return true
    }

    if (receiverType?.typeForValueClass == true) return true
    if (this is KaFunctionSymbol) {
        return valueParameters.any { it.returnType.typeForValueClass }
    }

    return false
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal val KaType.typeForValueClass: Boolean
    get() {
        val symbol = expandedSymbol as? KaNamedClassSymbol ?: return false
        return symbol.isInline
    }
