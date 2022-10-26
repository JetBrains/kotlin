/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiReferenceList
import org.jetbrains.kotlin.analysis.api.KtAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.allowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithTypeParameters
import org.jetbrains.kotlin.analysis.api.symbols.markers.isPrivateOrPrivateToThis
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.getKtModuleOfTypeSafe
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
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightConstructor
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightNoArgConstructor
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import java.util.*

@OptIn(KtAllowAnalysisOnEdt::class)
internal fun createSymbolLightClassNoCache(classOrObject: KtClassOrObject): KtLightClass? = allowAnalysisOnEdt {
    val anonymousObject = classOrObject.parent as? KtObjectLiteralExpression
    if (anonymousObject != null) {
        return analyzeForLightClasses(anonymousObject) {
            anonymousObject.getAnonymousObjectSymbol().createLightClassNoCache(anonymousObject.manager)
        }
    }

    return when {
        classOrObject is KtEnumEntry -> analyzeForLightClasses(classOrObject) {
            lightClassForEnumEntry(classOrObject)
        }

        classOrObject.hasModifier(INLINE_KEYWORD) -> {
            analyzeForLightClasses(classOrObject) {
                classOrObject.getNamedClassOrObjectSymbol()?.let { SymbolLightInlineClass(it, classOrObject.manager) }
            }
        }

        else -> {
            analyzeForLightClasses(classOrObject) {
                classOrObject.getClassOrObjectSymbol().createLightClassNoCache(classOrObject.manager)
            }
        }
    }
}


context(KtAnalysisSession)
internal fun KtClassOrObjectSymbol.createLightClassNoCache(manager: PsiManager): SymbolLightClassBase = when (this) {
    is KtAnonymousObjectSymbol -> SymbolLightAnonymousClass(this, manager)
    is KtNamedClassOrObjectSymbol -> when (classKind) {
        KtClassKind.INTERFACE -> SymbolLightInterfaceClass(this, manager)
        KtClassKind.ANNOTATION_CLASS -> SymbolLightAnnotationClass(this, manager)
        else -> SymbolLightClass(this, manager)
    }
}

context(KtAnalysisSession)
private fun lightClassForEnumEntry(ktEnumEntry: KtEnumEntry): KtLightClass? {
    if (ktEnumEntry.body == null) return null

    val symbolLightClass = ktEnumEntry.containingClass()?.toLightClass() as? SymbolLightClass ?: return null
    val targetField = symbolLightClass.ownFields.firstOrNull {
        it is SymbolLightFieldForEnumEntry && it.kotlinOrigin == ktEnumEntry
    } ?: return null

    return (targetField as? SymbolLightFieldForEnumEntry)?.initializingClass as? KtLightClass
}

context(KtAnalysisSession)
internal fun SymbolLightClassBase.createConstructors(
    declarations: Sequence<KtConstructorSymbol>,
    result: MutableList<KtLightMethod>
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
                constructorSymbol = constructor,
                lightMemberOrigin = null,
                containingClass = this@createConstructors,
                methodIndex = METHOD_INDEX_BASE
            )
        )
        createJvmOverloadsIfNeeded(constructor, result) { methodIndex, argumentSkipMask ->
            SymbolLightConstructor(
                constructorSymbol = constructor,
                lightMemberOrigin = null,
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

context(KtAnalysisSession)
private fun SymbolLightClassBase.defaultConstructor(): KtLightMethod {
    val classOrObject = kotlinOrigin
    val visibility = when {
        classOrObject is KtObjectDeclaration || isEnum -> PsiModifier.PRIVATE
        classOrObject?.hasModifier(SEALED_KEYWORD) == true -> PsiModifier.PROTECTED
        classOrObject is KtEnumEntry -> PsiModifier.PACKAGE_LOCAL
        else -> PsiModifier.PUBLIC
    }
    return noArgConstructor(visibility, METHOD_INDEX_FOR_DEFAULT_CTOR)
}

context(KtAnalysisSession)
private fun SymbolLightClassBase.noArgConstructor(
    visibility: String,
    methodIndex: Int,
): KtLightMethod {
    return SymbolLightNoArgConstructor(
        LightMemberOriginForDeclaration(
            originalElement = kotlinOrigin!!,
            originKind = JvmDeclarationOriginKind.OTHER,
            auxiliaryOriginalElement = kotlinOrigin as? KtDeclaration
        ),
        this,
        visibility,
        methodIndex
    )
}

context(KtAnalysisSession)
internal fun SymbolLightClassBase.createMethods(
    declarations: Sequence<KtCallableSymbol>,
    result: MutableList<KtLightMethod>,
    isTopLevel: Boolean = false,
    suppressStatic: Boolean = false
) {
    val (ctorProperties, regularMembers) = declarations.partition { it is KtPropertySymbol && it.isFromPrimaryConstructor }

    fun handleDeclaration(declaration: KtCallableSymbol) {
        when (declaration) {
            is KtFunctionSymbol -> {
                // TODO: check if it has expect modifier
                if (declaration.hasReifiedParameters ||
                    declaration.isHiddenOrSynthetic()
                ) return

                result.add(
                    SymbolLightSimpleMethod(
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
        handleDeclaration(it)
    }
    // Then, properties from the primary constructor parameters
    ctorProperties.forEach {
        handleDeclaration(it)
    }
}

context(KtAnalysisSession)
private inline fun <T : KtFunctionLikeSymbol> SymbolLightClassBase.createJvmOverloadsIfNeeded(
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
    if (declaration.origin == KtSymbolOrigin.SOURCE_MEMBER_GENERATED) return

    if (declaration.visibility.isPrivateOrPrivateToThis() &&
        declaration.getter?.hasBody == false &&
        declaration.setter?.hasBody == false
    ) return

    if (declaration.hasJvmFieldAnnotation()) return

    fun KtPropertyAccessorSymbol.needToCreateAccessor(siteTarget: AnnotationUseSiteTarget): Boolean {
        if (onlyJvmStatic && !hasJvmStaticAnnotation(siteTarget) && !declaration.hasJvmStaticAnnotation()) return false
        if (declaration.hasReifiedParameters) return false
        if (!hasBody && visibility.isPrivateOrPrivateToThis()) return false
        if (declaration.isHiddenOrSynthetic(siteTarget)) return false
        if (isHiddenOrSynthetic()) return false
        return true
    }

    val originalElement = declaration.psi as? KtDeclaration

    val getter = declaration.getter?.takeIf {
        it.needToCreateAccessor(AnnotationUseSiteTarget.PROPERTY_GETTER)
    }

    if (getter != null) {
        val lightMemberOrigin = originalElement?.let {
            LightMemberOriginForDeclaration(
                originalElement = it,
                originKind = JvmDeclarationOriginKind.OTHER,
                auxiliaryOriginalElement = getter.psi as? KtDeclaration
            )
        }

        result.add(
            SymbolLightAccessorMethod(
                propertyAccessorSymbol = getter,
                containingPropertySymbol = declaration,
                lightMemberOrigin = lightMemberOrigin,
                containingClass = this@createPropertyAccessors,
                isTopLevel = isTopLevel,
                suppressStatic = suppressStatic,
            )
        )
    }

    val setter = declaration.setter?.takeIf {
        !isAnnotationType && it.needToCreateAccessor(AnnotationUseSiteTarget.PROPERTY_SETTER)
    }

    if (isMutable && setter != null) {
        val lightMemberOrigin = originalElement?.let {
            LightMemberOriginForDeclaration(
                originalElement = it,
                originKind = JvmDeclarationOriginKind.OTHER,
                auxiliaryOriginalElement = setter.psi as? KtDeclaration
            )
        }
        result.add(
            SymbolLightAccessorMethod(
                propertyAccessorSymbol = setter,
                containingPropertySymbol = declaration,
                lightMemberOrigin = lightMemberOrigin,
                containingClass = this@createPropertyAccessors,
                isTopLevel = isTopLevel,
                suppressStatic = suppressStatic,
            )
        )
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
            propertySymbol = declaration,
            fieldName = fieldName,
            containingClass = this,
            lightMemberOrigin = null,
            isTopLevel = isTopLevel,
            forceStatic = forceStatic,
            takePropertyVisibility = takePropertyVisibility
        )
    )
}

context(KtAnalysisSession)
internal fun SymbolLightClassBase.createInheritanceList(forExtendsList: Boolean, superTypes: List<KtType>): PsiReferenceList {

    val role = if (forExtendsList) PsiReferenceList.Role.EXTENDS_LIST else PsiReferenceList.Role.IMPLEMENTS_LIST

    val listBuilder = KotlinSuperTypeListBuilder(
        kotlinOrigin = kotlinOrigin?.getSuperTypeList(),
        manager = manager,
        language = language,
        role = role
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
        it.createLightClassNoCache(manager)
    }


    val jvmDefaultMode =
        classOrObject?.getKtModuleOfTypeSafe<KtSourceModule>()?.languageVersionSettings?.getFlag(JvmAnalysisFlags.jvmDefaultMode)
            ?: JvmDefaultMode.DEFAULT

    if (this is KtNamedClassOrObjectSymbol &&
        classOrObject?.hasInterfaceDefaultImpls == true &&
        jvmDefaultMode != JvmDefaultMode.ALL_INCOMPATIBLE
    ) {
        result.add(SymbolLightClassForInterfaceDefaultImpls(this, containingClass, manager))
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

    val superClassSymbol = superClassOrigin.getClassOrObjectSymbol()

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

            if (subClassSymbol == superClassSymbol) return false

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
