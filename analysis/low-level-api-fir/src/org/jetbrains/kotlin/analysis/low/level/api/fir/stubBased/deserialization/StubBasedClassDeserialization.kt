/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.openapi.util.Key
import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.psi.stubs.Stub
import com.intellij.psi.stubs.StubTreeLoader
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsClassFinder
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.comparators.FirMemberDeclarationComparator
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.deserialization.addCloneForArrayIfNeeded
import org.jetbrains.kotlin.fir.deserialization.deserializationExtension
import org.jetbrains.kotlin.fir.resolve.transformers.setLazyPublishedVisibility
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeRigidType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.elements.KotlinValueClassRepresentation
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassStubImpl
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
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
        val stubTree = ClsClassFinder.allowMultifileClassPart {
            StubTreeLoader.getInstance().readOrBuild(ktElement.project, virtualFile, null)
        }

        stubList = stubTree?.plainList ?: emptyList()
        ktFile.putUserDataIfAbsent(STUBS_KEY, WeakReference(stubList))
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
    initialOrigin: FirDeclarationOrigin,
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
        isInline = classOrObject.hasModifier(KtTokens.INLINE_KEYWORD)
        isValue = classOrObject.hasModifier(KtTokens.VALUE_KEYWORD)
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

        classOrObject.primaryConstructor?.let { constructor ->
            addDeclaration(memberDeserializer.loadConstructor(constructor, classOrObject, this))
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

        addCloneForArrayIfNeeded(classId, context.dispatchReceiver, session)

        if (classId == StandardClassIds.Enum) {
            addCloneForEnumIfNeeded(classOrObject, context.dispatchReceiver)
        }

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

        contextParameters.addAll(memberDeserializer.createContextReceiversForClass(classOrObject, symbol))
    }.apply {
        if (classOrObject is KtClass && isInlineOrValue) {
            val stub = classOrObject.stub as? KotlinClassStubImpl ?: loadStubByElement(classOrObject)
            valueClassRepresentation = stub?.deserializeValueClassRepresentation(this)
        }

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

private fun KotlinClassStubImpl.deserializeValueClassRepresentation(klass: FirRegularClass): ValueClassRepresentation<ConeRigidType>? {
    val representation = valueClassRepresentation ?: return null
    val constructor = klass.declarations.firstNotNullOfOrNull { declaration ->
        (declaration as? FirConstructor)?.takeIf(FirConstructor::isPrimary)
    } ?: errorWithAttachment("Value class must have primary constructor") {
        withFirEntry("class", klass)
    }

    return when (representation) {
        KotlinValueClassRepresentation.INLINE_CLASS -> {
            val parameter = constructor.valueParameters.single()
            val type = parameter.coneRigidType()
            InlineClassRepresentation(parameter.name, type)
        }

        KotlinValueClassRepresentation.MULTI_FIELD_VALUE_CLASS -> {
            val mapping = constructor.valueParameters.map { parameter ->
                parameter.name to parameter.coneRigidType()
            }

            MultiFieldValueClassRepresentation(mapping)
        }
    }
}

private fun FirValueParameter.coneRigidType(): ConeRigidType {
    val type = returnTypeRef.coneType
    requireWithAttachment(type is ConeRigidType, { "Underlying type must be rigid type" }) {
        withConeTypeEntry("type", type)
        withFirEntry("valueParameter", this@coneRigidType)
    }

    return type
}

private fun FirRegularClassBuilder.addCloneForEnumIfNeeded(classOrObject: KtClassOrObject, dispatchReceiver: ConeClassLikeType?) {
    val hasCloneFunction = classOrObject.declarations
        .any { it is KtNamedFunction && it.name == "clone" && it.valueParameters.isEmpty() }

    if (hasCloneFunction) {
        return
    }

    val anyLookupId = StandardClassIds.Any.toLookupTag()
    val cloneCallableId = StandardClassIds.Callables.clone

    declarations += buildSimpleFunction {
        moduleData = this@addCloneForEnumIfNeeded.moduleData
        origin = this@addCloneForEnumIfNeeded.origin
        source = this@addCloneForEnumIfNeeded.source

        resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES

        returnTypeRef = buildResolvedTypeRef {
            coneType = ConeClassLikeTypeImpl(anyLookupId, typeArguments = emptyArray(), isMarkedNullable = false)
        }

        status = FirResolvedDeclarationStatusImpl(
            Visibilities.Protected,
            Modality.FINAL,
            EffectiveVisibility.Protected(anyLookupId)
        )

        name = cloneCallableId.callableName
        symbol = FirNamedFunctionSymbol(cloneCallableId)
        dispatchReceiverType = dispatchReceiver!!
    }
}
