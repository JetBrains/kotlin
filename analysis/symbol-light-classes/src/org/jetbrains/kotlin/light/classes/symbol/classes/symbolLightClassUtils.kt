/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiReferenceList
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithTypeParameters
import org.jetbrains.kotlin.analysis.api.symbols.markers.isPrivateOrPrivateToThis
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.getKtModuleOfTypeSafe
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.hasInterfaceDefaultImpls
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.*
import org.jetbrains.kotlin.light.classes.symbol.copy
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty
import org.jetbrains.kotlin.light.classes.symbol.isConst
import org.jetbrains.kotlin.light.classes.symbol.isLateInit
import org.jetbrains.kotlin.light.classes.symbol.mapType
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightConstructor
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightNoArgConstructor
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import java.util.*

internal fun createSymbolLightClassNoCache(classOrObject: KtClassOrObject, ktModule: KtModule): KtLightClass? = when {
    classOrObject.isObjectLiteral() -> SymbolLightClassForAnonymousObject(classOrObject, ktModule)
    classOrObject is KtEnumEntry -> lightClassForEnumEntry(classOrObject)
    else -> createLightClassNoCache(classOrObject, ktModule)
}

internal fun createLightClassNoCache(ktClassOrObject: KtClassOrObject, ktModule: KtModule): SymbolLightClassBase = when {
    ktClassOrObject.hasModifier(INLINE_KEYWORD) -> SymbolLightClassForInlineClass(ktClassOrObject, ktModule)
    ktClassOrObject is KtClass && ktClassOrObject.isAnnotation() -> SymbolLightClassForAnnotationClass(ktClassOrObject, ktModule)
    ktClassOrObject is KtClass && ktClassOrObject.isInterface() -> SymbolLightClassForInterface(ktClassOrObject, ktModule)
    else -> SymbolLightClassForClassOrObject(ktClassOrObject, ktModule)
}

context(KtAnalysisSession)
internal fun createLightClassNoCache(
    ktClassOrObjectSymbol: KtNamedClassOrObjectSymbol,
    ktModule: KtModule,
    manager: PsiManager,
): SymbolLightClassBase = when (ktClassOrObjectSymbol.classKind) {
    KtClassKind.INTERFACE -> SymbolLightClassForInterface(
        ktAnalysisSession = this@KtAnalysisSession,
        ktModule = ktModule,
        classOrObjectSymbol = ktClassOrObjectSymbol,
        manager = manager,
    )

    KtClassKind.ANNOTATION_CLASS -> SymbolLightClassForAnnotationClass(
        ktAnalysisSession = this@KtAnalysisSession,
        ktModule = ktModule,
        classOrObjectSymbol = ktClassOrObjectSymbol,
        manager = manager,
    )

    else -> SymbolLightClassForClassOrObject(
        ktAnalysisSession = this@KtAnalysisSession,
        ktModule = ktModule,
        classOrObjectSymbol = ktClassOrObjectSymbol,
        manager = manager,
    )
}

private fun lightClassForEnumEntry(ktEnumEntry: KtEnumEntry): KtLightClass? {
    if (ktEnumEntry.body == null) return null

    val symbolLightClass = ktEnumEntry.containingClass()?.toLightClass() as? SymbolLightClassForClassOrObject ?: return null
    val targetField = symbolLightClass.ownFields.firstOrNull {
        it is SymbolLightFieldForEnumEntry && it.kotlinOrigin == ktEnumEntry
    } ?: return null

    return (targetField as? SymbolLightFieldForEnumEntry)?.initializingClass as? KtLightClass
}

context(KtAnalysisSession)
internal fun SymbolLightClassBase.createConstructors(
    declarations: Sequence<KtConstructorSymbol>,
    result: MutableList<KtLightMethod>,
) {
    val constructors = declarations.toList()
    if (constructors.isEmpty()) {
        result.add(defaultConstructor())
        return
    }

    for (constructor in constructors) {
        if (constructor.isHiddenOrSynthetic()) continue

        result.add(
            SymbolLightConstructor(
                ktAnalysisSession = this@KtAnalysisSession,
                constructorSymbol = constructor,
                containingClass = this@createConstructors,
                methodIndex = METHOD_INDEX_BASE
            )
        )

        createJvmOverloadsIfNeeded(constructor, result) { methodIndex, argumentSkipMask ->
            SymbolLightConstructor(
                ktAnalysisSession = this@KtAnalysisSession,
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
            noArgConstructor(primaryConstructor.visibility.externalDisplayName, METHOD_INDEX_FOR_NO_ARG_OVERLOAD_CTOR)
        )
    }
}

context(KtAnalysisSession)
private fun SymbolLightClassBase.shouldGenerateNoArgOverload(
    primaryConstructor: KtConstructorSymbol,
    constructors: Iterable<KtConstructorSymbol>,
): Boolean {
    val classOrObject = kotlinOrigin ?: return false
    return !primaryConstructor.visibility.isPrivateOrPrivateToThis() &&
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
        this is SymbolLightClassForClassLike<*> && (isObject || isEnum) -> PsiModifier.PRIVATE
        classOrObject?.hasModifier(SEALED_KEYWORD) == true -> PsiModifier.PROTECTED
        this is SymbolLightClassForEnumEntry -> PsiModifier.PACKAGE_LOCAL
        else -> PsiModifier.PUBLIC
    }

    return noArgConstructor(visibility, METHOD_INDEX_FOR_DEFAULT_CTOR)
}

private fun SymbolLightClassBase.noArgConstructor(
    visibility: String,
    methodIndex: Int,
): KtLightMethod = SymbolLightNoArgConstructor(
    kotlinOrigin?.let {
        LightMemberOriginForDeclaration(
            originalElement = it,
            originKind = JvmDeclarationOriginKind.OTHER,
        )
    },
    this,
    visibility,
    methodIndex,
)

context(KtAnalysisSession)
internal fun SymbolLightClassBase.createMethods(
    declarations: Sequence<KtCallableSymbol>,
    result: MutableList<KtLightMethod>,
    isTopLevel: Boolean = false,
    suppressStatic: Boolean = false
) {
    val (ctorProperties, regularMembers) = declarations.partition { it is KtPropertySymbol && it.isFromPrimaryConstructor }

    fun KtAnalysisSession.handleDeclaration(declaration: KtCallableSymbol) {
        when (declaration) {
            is KtFunctionSymbol -> {
                // TODO: check if it has expect modifier
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

            is KtPropertySymbol -> createPropertyAccessors(
                result,
                declaration,
                isTopLevel = isTopLevel,
                suppressStatic = suppressStatic
            )

            is KtConstructorSymbol -> error("Constructors should be handled separately and not passed to this function")
            else -> {}
        }
    }

    // Regular members
    regularMembers.forEach {
        this@KtAnalysisSession.handleDeclaration(it)
    }
    // Then, properties from the primary constructor parameters
    ctorProperties.forEach {
        this@KtAnalysisSession.handleDeclaration(it)
    }
}

context(KtAnalysisSession)
private inline fun <T : KtFunctionLikeSymbol> createJvmOverloadsIfNeeded(
    declaration: T,
    result: MutableList<KtLightMethod>,
    lightMethodCreator: (Int, BitSet) -> KtLightMethod
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

context(KtAnalysisSession)
internal fun SymbolLightClassBase.createPropertyAccessors(
    result: MutableList<KtLightMethod>,
    declaration: KtPropertySymbol,
    isTopLevel: Boolean,
    isMutable: Boolean = !declaration.isVal,
    onlyJvmStatic: Boolean = false,
    suppressStatic: Boolean = false,
) {
    if (declaration is KtKotlinPropertySymbol && declaration.isConst) return
    if (declaration.name.isSpecial) return

    if (declaration.visibility.isPrivateOrPrivateToThis() &&
        declaration.getter?.hasBody == false &&
        declaration.setter?.hasBody == false
    ) return

    if (declaration.hasJvmFieldAnnotation()) return

    fun KtPropertyAccessorSymbol.needToCreateAccessor(siteTarget: AnnotationUseSiteTarget): Boolean {
        if (onlyJvmStatic &&
            !hasJvmStaticAnnotation(siteTarget, strictUseSite = false) &&
            !declaration.hasJvmStaticAnnotation(siteTarget, strictUseSite = false)
        ) return false

        if (declaration.hasReifiedParameters) return false
        if (!hasBody && visibility.isPrivateOrPrivateToThis()) return false
        if (declaration.isHiddenOrSynthetic(siteTarget)) return false
        return !isHiddenOrSynthetic(siteTarget, strictUseSite = false)
    }

    val originalElement = declaration.sourcePsiSafe<KtDeclaration>()

    val getter = declaration.getter?.takeIf {
        it.needToCreateAccessor(AnnotationUseSiteTarget.PROPERTY_GETTER)
    }

    fun createSymbolLightAccessorMethod(accessor: KtPropertyAccessorSymbol): SymbolLightAccessorMethod {
        val lightMemberOrigin = originalElement?.let {
            LightMemberOriginForDeclaration(
                originalElement = it,
                originKind = JvmDeclarationOriginKind.OTHER,
                auxiliaryOriginalElement = accessor.sourcePsiSafe<KtDeclaration>()
            )
        }

        return SymbolLightAccessorMethod(
            ktAnalysisSession = this@KtAnalysisSession,
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

context(KtAnalysisSession)
internal fun SymbolLightClassBase.createField(
    declaration: KtPropertySymbol,
    nameGenerator: SymbolLightField.FieldNameGenerator,
    isTopLevel: Boolean,
    forceStatic: Boolean,
    takePropertyVisibility: Boolean,
    result: MutableList<KtLightField>
) {

    fun hasBackingField(property: KtPropertySymbol): Boolean = when (property) {
        is KtSyntheticJavaPropertySymbol -> true
        is KtKotlinPropertySymbol -> when {
            property.origin == KtSymbolOrigin.SOURCE_MEMBER_GENERATED -> false
            property.modality == Modality.ABSTRACT -> false
            property.isHiddenOrSynthetic() -> false
            property.isLateInit -> true
            property.isDelegatedProperty -> true
            property.isFromPrimaryConstructor -> true
            property.psi.let { it == null || it is KtParameter } -> true
            property.hasJvmSyntheticAnnotation(AnnotationUseSiteTarget.FIELD) -> false
            else -> property.hasBackingField
        }
    }

    if (!hasBackingField(declaration)) return

    val isDelegated = (declaration as? KtKotlinPropertySymbol)?.isDelegatedProperty == true
    val fieldName = nameGenerator.generateUniqueFieldName(
        declaration.name.asString() + (if (isDelegated) JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX else "")
    )

    result.add(
        SymbolLightFieldForProperty(
            ktAnalysisSession = this@KtAnalysisSession,
            propertySymbol = declaration,
            fieldName = fieldName,
            containingClass = this,
            lightMemberOrigin = null,
            isTopLevel = isTopLevel,
            forceStatic = forceStatic,
            takePropertyVisibility = takePropertyVisibility,
        )
    )
}

context(KtAnalysisSession)
internal fun SymbolLightClassForClassLike<*>.createInheritanceList(
    forExtendsList: Boolean,
    superTypes: List<KtType>,
): PsiReferenceList {
    val role = if (forExtendsList) PsiReferenceList.Role.EXTENDS_LIST else PsiReferenceList.Role.IMPLEMENTS_LIST

    val listBuilder = KotlinSuperTypeListBuilder(
        kotlinOrigin = kotlinOrigin?.getSuperTypeList(),
        manager = manager,
        language = language,
        role = role,
    )

    fun KtType.needToAddTypeIntoList(): Boolean {
        if (this !is KtNonErrorClassType) return false

        // Do not add redundant "extends java.lang.Object" anywhere
        if (this.classId == StandardClassIds.Any) return false

        // We don't have Enum among enums supertype in sources neither we do for decompiled class-files and light-classes
        if (isEnum && this.classId == StandardClassIds.Enum) return false

        // Interfaces have only extends lists
        if (isInterface) return forExtendsList

        val classKind = (classSymbol as? KtClassOrObjectSymbol)?.classKind
        val isJvmInterface = classKind == KtClassKind.INTERFACE || classKind == KtClassKind.ANNOTATION_CLASS

        return forExtendsList == !isJvmInterface
    }

    superTypes.asSequence()
        .filter { it.needToAddTypeIntoList() }
        .forEach { superType ->
            if (superType !is KtNonErrorClassType) return@forEach
            val mappedType =
                mapType(superType, this@createInheritanceList, KtTypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS)
                    ?: return@forEach
            listBuilder.addReference(mappedType)
            if (mappedType.canonicalText.startsWith("kotlin.collections.")) {
                val mappedToNoCollectionAsIs = mapType(superType, this@createInheritanceList, KtTypeMappingMode.SUPER_TYPE)
                if (mappedToNoCollectionAsIs != null &&
                    mappedType.canonicalText != mappedToNoCollectionAsIs.canonicalText
                ) {
                    // Add java supertype
                    listBuilder.addReference(mappedToNoCollectionAsIs)
                    // Add marker interface
                    listBuilder.addMarkerInterfaceIfNeeded(superType.classId)
                }
            }
        }

    return listBuilder
}

context(KtAnalysisSession)
internal fun KtSymbolWithMembers.createInnerClasses(
    manager: PsiManager,
    containingClass: SymbolLightClassBase,
    classOrObject: KtClassOrObject?
): List<SymbolLightClassBase> {
    val result = ArrayList<SymbolLightClassBase>()

    // workaround for ClassInnerStuffCache not supporting classes with null names, see KT-13927
    // inner classes with null names can't be searched for and can't be used from java anyway
    // we can't prohibit creating light classes with null names either since they can contain members

    getDeclaredMemberScope().getClassifierSymbols().filterIsInstance<KtNamedClassOrObjectSymbol>().mapTo(result) {
        val classOrObjectDeclaration = it.psiSafe<KtClassOrObject>()
        if (classOrObjectDeclaration != null) {
            createLightClassNoCache(classOrObjectDeclaration, containingClass.ktModule)
        } else {
            createLightClassNoCache(it, ktModule = containingClass.ktModule, manager)
        }
    }

    val jvmDefaultMode = classOrObject
        ?.getKtModuleOfTypeSafe<KtSourceModule>()
        ?.languageVersionSettings
        ?.getFlag(JvmAnalysisFlags.jvmDefaultMode)
        ?: JvmDefaultMode.DEFAULT

    if (containingClass is SymbolLightClassForInterface &&
        classOrObject?.hasInterfaceDefaultImpls == true &&
        jvmDefaultMode != JvmDefaultMode.ALL_INCOMPATIBLE
    ) {
        result.add(SymbolLightClassForInterfaceDefaultImpls(containingClass))
    }

    if (containingClass is SymbolLightClassForAnnotationClass &&
        this is KtNamedClassOrObjectSymbol &&
        hasAnnotation(StandardNames.FqNames.repeatable, annotationUseSiteTarget = null) &&
        !hasAnnotation(JvmAnnotationNames.REPEATABLE_ANNOTATION, annotationUseSiteTarget = null)
    ) {
        result.add(SymbolLightClassForRepeatableAnnotationContainer(containingClass))
    }

    return result
}

context(KtAnalysisSession)
internal fun KtClassOrObject.checkIsInheritor(superClassOrigin: KtClassOrObject, checkDeep: Boolean): Boolean {
    if (this == superClassOrigin) return false
    if (superClassOrigin is KtEnumEntry) {
        return false // enum entry cannot have inheritors
    }
    if (!superClassOrigin.canBeAnalysed()) {
        return false
    }

    val superClassSymbol = superClassOrigin.getClassOrObjectSymbol() ?: return false

    when (this) {
        is KtEnumEntry -> {
            val enumEntrySymbol = this.getEnumEntrySymbol()
            val classId = enumEntrySymbol.containingEnumClassIdIfNonLocal ?: return false
            val enumClassSymbol = getClassOrObjectSymbolByClassId(classId) ?: return false
            if (enumClassSymbol == superClassSymbol) return true
            return if (checkDeep) {
                enumClassSymbol.isSubClassOf(superClassSymbol)
            } else {
                false
            }
        }

        else -> {
            val subClassSymbol = this.getClassOrObjectSymbol()

            if (subClassSymbol == null || subClassSymbol == superClassSymbol) return false

            return if (checkDeep) {
                subClassSymbol.isSubClassOf(superClassSymbol)
            } else {
                subClassSymbol.isDirectSubClassOf(superClassSymbol)
            }
        }
    }
}

private val KtSymbolWithTypeParameters.hasReifiedParameters: Boolean
    get() = typeParameters.any { it.isReified }

context(KtAnalysisSession)
internal fun SymbolLightClassBase.addPropertyBackingFields(
    result: MutableList<KtLightField>,
    symbolWithMembers: KtSymbolWithMembers,
) {
    val propertySymbols = symbolWithMembers.getDeclaredMemberScope().getCallableSymbols()
        .filterIsInstance<KtPropertySymbol>()
        .applyIf(symbolWithMembers is KtClassOrObjectSymbol && symbolWithMembers.classKind == KtClassKind.COMPANION_OBJECT) {
            // All fields for companion object of classes are generated to the containing class
            // For interfaces, only @JvmField-annotated properties are generated to the containing class
            // Probably, the same should work for const vals but it doesn't at the moment (see KT-28294)
            filter { containingClass?.isInterface == true && !it.hasJvmFieldAnnotation() }
        }

    val propertyGroups = propertySymbols.groupBy { it.isFromPrimaryConstructor }

    val nameGenerator = SymbolLightField.FieldNameGenerator()

    val forceStatic = symbolWithMembers is KtClassOrObjectSymbol && symbolWithMembers.classKind.isObject
    fun addPropertyBackingField(propertySymbol: KtPropertySymbol) {
        val isJvmField = propertySymbol.hasJvmFieldAnnotation()
        val isLateInit = propertySymbol.isLateInit
        val isConst = propertySymbol.isConst

        val takePropertyVisibility = isLateInit || isJvmField || isConst

        createField(
            declaration = propertySymbol,
            nameGenerator = nameGenerator,
            isTopLevel = false,
            forceStatic = forceStatic,
            takePropertyVisibility = takePropertyVisibility,
            result = result
        )
    }

    // First, properties from parameters
    propertyGroups[true]?.forEach(::addPropertyBackingField)
    // Then, regular member properties
    propertyGroups[false]?.forEach(::addPropertyBackingField)
}
