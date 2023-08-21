/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.psi.stubs.Stub
import com.intellij.psi.stubs.StubTreeLoader
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.kotlin.KtFakeSourceElement
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.createDataClassCopyFunction
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.comparators.FirMemberDeclarationComparator
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.addDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.deserialization.addCloneForArrayIfNeeded
import org.jetbrains.kotlin.fir.deserialization.deserializationExtension
import org.jetbrains.kotlin.fir.resolve.transformers.setLazyPublishedVisibility
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import java.lang.ref.WeakReference

internal val KtModifierListOwner.visibility: Visibility
    get() = with(modifierList) {
        when {
            this == null -> Visibilities.Public
            hasModifier(KtTokens.PRIVATE_KEYWORD) -> Visibilities.Private
            hasModifier(KtTokens.PUBLIC_KEYWORD) -> Visibilities.Public
            hasModifier(KtTokens.PROTECTED_KEYWORD) -> Visibilities.Protected
            else -> if (hasModifier(KtTokens.INTERNAL_KEYWORD)) Visibilities.Internal else Visibilities.Public
        }
    }

internal val KtDeclaration.modality: Modality
    get() {
        return when {
            hasModifier(KtTokens.SEALED_KEYWORD) -> Modality.SEALED
            hasModifier(KtTokens.ABSTRACT_KEYWORD) || this is KtClass && isInterface() -> Modality.ABSTRACT
            hasModifier(KtTokens.OPEN_KEYWORD) -> Modality.OPEN
            else -> Modality.FINAL
        }
    }

private val STUBS_KEY = Key.create<WeakReference<List<Stub>?>>("STUBS")
internal fun <S, T> loadStubByElement(ktElement: T): S? where T : StubBasedPsiElementBase<*>, T : KtElement {
    ktElement.greenStub?.let {
        @Suppress("UNCHECKED_CAST")
        return it as S
    }
    val ktFile = ktElement.containingKtFile
    require(ktFile.isCompiled) {
        "Expected compiled file $ktFile"
    }
    val virtualFile = PsiUtilCore.getVirtualFile(ktFile) ?: return null
    var stubList = ktFile.getUserData(STUBS_KEY)?.get()
    if (stubList == null) {
        val stubTree = StubTreeLoader.getInstance().readOrBuild(ktElement.project, virtualFile, null)
        stubList = stubTree?.plainList ?: emptyList()
        ktFile.putUserData(STUBS_KEY, WeakReference(stubList))
    }
    val nodeList = (ktFile.node as FileElement).stubbedSpine.spineNodes
    if (stubList.size != nodeList.size) return null
    @Suppress("UNCHECKED_CAST")
    return stubList[nodeList.indexOf(ktElement.node)] as S
}

internal fun deserializeClassToSymbol(
    classId: ClassId,
    classOrObject: KtClassOrObject,
    symbol: FirRegularClassSymbol,
    session: FirSession,
    moduleData: FirModuleData,
    defaultAnnotationDeserializer: StubBasedAnnotationDeserializer?,
    scopeProvider: FirScopeProvider,
    parentContext: StubBasedFirDeserializationContext? = null,
    containerSource: DeserializedContainerSource? = null,
    deserializeNestedClass: (ClassId, StubBasedFirDeserializationContext) -> FirRegularClassSymbol?,
    initialOrigin: FirDeclarationOrigin
) {
    val kind = when (classOrObject) {
        is KtObjectDeclaration -> ClassKind.OBJECT
        is KtClass -> when {
            classOrObject.isInterface() -> ClassKind.INTERFACE
            classOrObject.isEnum() -> ClassKind.ENUM_CLASS
            classOrObject.isAnnotation() -> ClassKind.ANNOTATION_CLASS
            else -> ClassKind.CLASS
        }
        else -> errorWithAttachment("Unexpected class or object: ${classOrObject::class}") {
            withPsiEntry("class", classOrObject)
        }
    }
    val modality = classOrObject.modality
    val visibility = classOrObject.visibility
    val status = FirResolvedDeclarationStatusImpl(
        visibility,
        modality,
        visibility.toEffectiveVisibility(parentContext?.outerClassSymbol, forClass = true)
    ).apply {
        isExpect = classOrObject.hasModifier(KtTokens.EXPECT_KEYWORD)
        isActual = false
        isInner = classOrObject.hasModifier(KtTokens.INNER_KEYWORD)
        isCompanion = (classOrObject as? KtObjectDeclaration)?.isCompanion() == true
        isData = classOrObject.hasModifier(KtTokens.DATA_KEYWORD)
        isInline = classOrObject.hasModifier(KtTokens.INLINE_KEYWORD) || classOrObject.hasModifier(KtTokens.VALUE_KEYWORD)
        isFun = classOrObject.hasModifier(KtTokens.FUN_KEYWORD)
        isExternal = classOrObject.hasModifier(KtTokens.EXTERNAL_KEYWORD)
    }
    val annotationDeserializer = defaultAnnotationDeserializer ?: StubBasedAnnotationDeserializer(session)
    val context =
        parentContext?.childContext(
            classOrObject,
            classId.relativeClassName,
            containerSource,
            symbol,
            annotationDeserializer,
            status.isInner
        ) ?: StubBasedFirDeserializationContext.createForClass(
            classId,
            classOrObject,
            moduleData,
            annotationDeserializer,
            containerSource,
            symbol,
            initialOrigin
        )
    buildRegularClass {
        source = KtRealPsiSourceElement(classOrObject)
        this.moduleData = moduleData
        this.origin = initialOrigin
        name = classId.shortClassName
        this.status = status
        classKind = kind
        this.scopeProvider = scopeProvider
        this.symbol = symbol

        resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES

        typeParameters += context.typeDeserializer.ownTypeParameters.map { it.fir }
        if (status.isInner)
            typeParameters += parentContext?.allTypeParameters?.map { buildOuterClassTypeParameterRef { this.symbol = it } }.orEmpty()

        val typeDeserializer = context.typeDeserializer
        val memberDeserializer = context.memberDeserializer

        val superTypeList = classOrObject.getSuperTypeList()
        if (superTypeList != null) {
            superTypeRefs.addAll(superTypeList.entries.map { superTypeReference ->
                typeDeserializer.typeRef(
                    superTypeReference.typeReference
                        ?: errorWithAttachment("Super entry doesn't have type reference") {
                            withPsiEntry("superTypeReference", superTypeReference)
                        }
                )
            })
        } else if (StandardClassIds.Any != classId && StandardClassIds.Nothing != classId) {
            superTypeRefs.add(session.builtinTypes.anyType)
        }

        val firPrimaryConstructor = classOrObject.primaryConstructor?.let {
            val constructor = memberDeserializer.loadConstructor(it, classOrObject, this)
            addDeclaration(constructor)
            constructor
        }

        classOrObject.body?.declarations?.forEach { declaration ->
            when (declaration) {
                is KtConstructor<*> -> addDeclaration(memberDeserializer.loadConstructor(declaration, classOrObject, this))
                is KtNamedFunction -> addDeclaration(memberDeserializer.loadFunction(declaration, symbol, session))
                is KtProperty -> addDeclaration(memberDeserializer.loadProperty(declaration, symbol))
                is KtEnumEntry -> addDeclaration(memberDeserializer.loadEnumEntry(declaration, symbol, classId))
                is KtClassOrObject -> {
                    val name = declaration.name ?: errorWithAttachment("Class doesn't have name $declaration") {
                        withPsiEntry("class", declaration)
                    }

                    val nestedClassId = classId.createNestedClassId(Name.identifier(name))
                    // Add declaration to the context to avoid redundant provider access to the class map
                    deserializeNestedClass(nestedClassId, context.withClassLikeDeclaration(declaration))?.fir?.let(this::addDeclaration)
                }

                is KtTypeAlias -> addDeclaration(memberDeserializer.loadTypeAlias(declaration, FirTypeAliasSymbol(classId)))
            }
        }

        if (classKind == ClassKind.ENUM_CLASS) {
            generateValuesFunction(
                moduleData,
                classId.packageFqName,
                classId.relativeClassName,
                origin = initialOrigin
            )
            generateValueOfFunction(moduleData, classId.packageFqName, classId.relativeClassName, origin = initialOrigin)
            generateEntriesGetter(moduleData, classId.packageFqName, classId.relativeClassName, origin = initialOrigin)
        }

        if (classOrObject.isData() && firPrimaryConstructor != null) {
            val zippedParameters =
                classOrObject.primaryConstructorParameters zip declarations.filterIsInstance<FirProperty>()
            addDeclaration(
                createDataClassCopyFunction(
                    classId,
                    classOrObject,
                    context.dispatchReceiver,
                    zippedParameters,
                    createClassTypeRefWithSourceKind = { firPrimaryConstructor.returnTypeRef.copyWithNewSourceKind(it) },
                    createParameterTypeRefWithSourceKind = { property, newKind ->
                        property.returnTypeRef.copyWithNewSourceKind(newKind)
                    },
                    toFirSource = { src, kind -> KtFakeSourceElement(src as PsiElement, kind) },
                    addValueParameterAnnotations = { annotations += context.annotationDeserializer.loadAnnotations(it) },
                    isVararg = { it.isVarArg }
                )
            )
        }

        addCloneForArrayIfNeeded(classId, context.dispatchReceiver, session)
        session.deserializationExtension?.run {
            configureDeserializedClass(classId)
        }

        declarations.sortWith(object : Comparator<FirDeclaration> {
            override fun compare(a: FirDeclaration, b: FirDeclaration): Int {
                // Reorder members based on their type and name only.
                // See FE 1.0's [DeserializedMemberScope#addMembers].
                if (a is FirMemberDeclaration && b is FirMemberDeclaration) {
                    return FirMemberDeclarationComparator.TypeAndNameComparator.compare(a, b)
                }
                return 0
            }
        })
        companionObjectSymbol = (declarations.firstOrNull { it is FirRegularClass && it.isCompanion } as FirRegularClass?)?.symbol

        contextReceivers.addAll(memberDeserializer.createContextReceiversForClass(classOrObject))
    }.apply {
        valueClassRepresentation = computeValueClassRepresentation(this, session)

        replaceAnnotations(
            context.annotationDeserializer.loadAnnotations(classOrObject)
        )

        sourceElement = containerSource

        replaceDeprecationsProvider(getDeprecationsProvider(session))

        setLazyPublishedVisibility(
            hasPublishedApi = classOrObject.annotationEntries.any { context.annotationDeserializer.getAnnotationClassId(it) == StandardClassIds.Annotations.PublishedApi },
            parentProperty = null,
            session
        )
    }
}
