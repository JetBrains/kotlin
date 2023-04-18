/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiReferenceList
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.hasAnnotation
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithTypeParameters
import org.jetbrains.kotlin.analysis.api.symbols.markers.isPrivateOrPrivateToThis
import org.jetbrains.kotlin.analysis.api.types.KtClassErrorType
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.hasInterfaceDefaultImpls
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.*
import org.jetbrains.kotlin.light.classes.symbol.copy
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty
import org.jetbrains.kotlin.light.classes.symbol.mapType
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAnnotationsMethod
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightConstructor
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightNoArgConstructor
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod
import org.jetbrains.kotlin.load.java.JvmAbi
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
        this is SymbolLightClassForClassLike<*> && (classKind().let { it.isObject || it == KtClassKind.ENUM_CLASS }) -> PsiModifier.PRIVATE
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

    if (declaration.getter?.hasBody != true && declaration.setter?.hasBody != true && declaration.visibility.isPrivateOrPrivateToThis()) return

    val originalElement = declaration.sourcePsiSafe<KtDeclaration>()

    val generatePropertyAnnotationsMethods =
        (declaration.getContainingModule() as? KtSourceModule)
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
            ktAnalysisSession = this@KtAnalysisSession,
            containingPropertySymbol = declaration,
            lightMemberOrigin = lightMemberOrigin,
            containingClass = this@createPropertyAccessors
        )
        if (method.annotations.size > 1) { // There's always a @java.lang.Deprecated
            result.add(method)
        }
    }

    if (declaration.isJvmField) return
    val propertyTypeIsValueClass = declaration.hasTypeForValueClassInSignature()

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
    if (this !is SymbolLightClassForFacade && propertyTypeIsValueClass) return

    fun KtPropertyAccessorSymbol.needToCreateAccessor(siteTarget: AnnotationUseSiteTarget): Boolean {
        val useSiteTargetFilterForPropertyAccessor = siteTarget.toOptionalFilter()
        if (onlyJvmStatic &&
            !hasJvmStaticAnnotation(useSiteTargetFilterForPropertyAccessor) &&
            !declaration.hasJvmStaticAnnotation(useSiteTargetFilterForPropertyAccessor)
        ) return false

        if (declaration.hasReifiedParameters) return false
        if (!hasBody && visibility.isPrivateOrPrivateToThis()) return false
        if (declaration.isHiddenOrSynthetic(siteTarget)) return false
        return !isHiddenOrSynthetic(siteTarget, useSiteTargetFilterForPropertyAccessor)
    }

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
        !isAnnotationType && it.needToCreateAccessor(AnnotationUseSiteTarget.PROPERTY_SETTER) && !propertyTypeIsValueClass
    }

    if (isMutable && setter != null) {
        result.add(createSymbolLightAccessorMethod(setter))
    }
}

context(KtAnalysisSession)
internal fun SymbolLightClassBase.createField(
    declaration: KtPropertySymbol,
    nameGenerator: SymbolLightField.FieldNameGenerator,
    isStatic: Boolean,
    result: MutableList<KtLightField>
) {
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
            isStatic = isStatic,
        )
    )
}

context(KtAnalysisSession)
private fun hasBackingField(property: KtPropertySymbol): Boolean {
    if (property is KtSyntheticJavaPropertySymbol) return true
    requireIsInstance<KtKotlinPropertySymbol>(property)

    if (property.origin.cannotHasBackingField() || property.isStatic) return false
    if (property.isLateInit || property.isDelegatedProperty || property.isFromPrimaryConstructor) return true
    val hasBackingFieldByPsi: Boolean? = property.psi?.hasBackingField()
    if (hasBackingFieldByPsi == false) {
        return hasBackingFieldByPsi
    }

    val fieldUseSite = AnnotationUseSiteTarget.FIELD
    if (property.modality == Modality.ABSTRACT ||
        property.isHiddenOrSynthetic(fieldUseSite, fieldUseSite.toOptionalFilter())
    ) return false

    return hasBackingFieldByPsi ?: property.hasBackingField
}

private fun KtSymbolOrigin.cannotHasBackingField(): Boolean =
    this == KtSymbolOrigin.SOURCE_MEMBER_GENERATED ||
            this == KtSymbolOrigin.DELEGATED ||
            this == KtSymbolOrigin.INTERSECTION_OVERRIDE ||
            this == KtSymbolOrigin.SUBSTITUTION_OVERRIDE

private fun PsiElement.hasBackingField(): Boolean {
    if (this is KtParameter) return true
    if (this !is KtProperty) return false

    return hasInitializer() || getter?.takeIf { it.hasBody() } == null || setter?.takeIf { it.hasBody() } == null && isVar
}

context(KtAnalysisSession)
internal fun SymbolLightClassForClassLike<*>.createInheritanceList(
    forExtendsList: Boolean,
    superTypes: List<KtType>,
): PsiReferenceList {
    val role = if (forExtendsList) PsiReferenceList.Role.EXTENDS_LIST else PsiReferenceList.Role.IMPLEMENTS_LIST

    val listBuilder = KotlinSuperTypeListBuilder(
        this,
        kotlinOrigin = kotlinOrigin?.getSuperTypeList(),
        manager = manager,
        language = language,
        role = role,
    )

    fun KtType.needToAddTypeIntoList(): Boolean {
        // Do not add redundant "extends java.lang.Object" anywhere
        if (this.isAny) return false
        // Interfaces have only extends lists
        if (isInterface) return forExtendsList

        return when (this) {
            is KtNonErrorClassType -> {
                // We don't have Enum among enums supertype in sources neither we do for decompiled class-files and light-classes
                if (isEnum && this.classId == StandardClassIds.Enum) return false

                // NB: need to expand type alias, e.g., kotlin.Comparator<T> -> java.util.Comparator<T>
                val classKind = expandedClassSymbol?.classKind
                val isJvmInterface = classKind == KtClassKind.INTERFACE || classKind == KtClassKind.ANNOTATION_CLASS

                forExtendsList == !isJvmInterface
            }

            is KtClassErrorType -> {
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
                KtTypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS
            ) ?: return@forEach
            listBuilder.addReference(mappedType)
            if (mappedType.canonicalText.startsWith("kotlin.collections.")) {
                val mappedToNoCollectionAsIs = mapType(superType, this@createInheritanceList, KtTypeMappingMode.SUPER_TYPE)
                if (mappedToNoCollectionAsIs != null &&
                    mappedType.canonicalText != mappedToNoCollectionAsIs.canonicalText
                ) {
                    // Add java supertype
                    listBuilder.addReference(mappedToNoCollectionAsIs)
                    // Add marker interface
                    if (superType is KtNonErrorClassType) {
                        listBuilder.addMarkerInterfaceIfNeeded(superType.classId)
                    }
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
        ?.let { getModule(it) as? KtSourceModule }
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
        hasAnnotation(StandardClassIds.Annotations.Repeatable) &&
        !hasAnnotation(StandardClassIds.Annotations.Java.Repeatable)
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
            filter { containingClass?.isInterface == true && !it.isJvmField }
        }

    val (ctorProperties, memberProperties) = propertySymbols.partition { it.isFromPrimaryConstructor }

    val nameGenerator = SymbolLightField.FieldNameGenerator()

    val isStatic = symbolWithMembers is KtClassOrObjectSymbol && symbolWithMembers.classKind.isObject
    fun addPropertyBackingField(propertySymbol: KtPropertySymbol) {
        createField(
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

context(KtAnalysisSession)
internal fun KtCallableSymbol.hasTypeForValueClassInSignature(ignoreReturnType: Boolean = false): Boolean {
    if (!ignoreReturnType) {
        val psiDeclaration = sourcePsiSafe<KtCallableDeclaration>()
        if (psiDeclaration?.typeReference != null && returnType.typeForValueClass) return true
    }

    if (receiverType?.typeForValueClass == true) return true
    if (this is KtFunctionLikeSymbol) {
        return valueParameters.any { it.returnType.typeForValueClass }
    }

    return false
}

context(KtAnalysisSession)
internal val KtType.typeForValueClass: Boolean
    get() {
        val symbol = expandedClassSymbol as? KtNamedClassOrObjectSymbol ?: return false
        return symbol.isInline
    }
