// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.analysis.decompiler.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.analysis.decompiler.stub.flags.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.MemberKind
import org.jetbrains.kotlin.metadata.ProtoBuf.Modality
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.KotlinConstructorStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyStubImpl
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.serialization.deserialization.AnnotatedCallableKind
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.utils.addToStdlib.runIf

fun createPackageDeclarationsStubs(
    parentStub: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer.Package,
    packageProto: ProtoBuf.Package
) {
    createDeclarationsStubs(parentStub, outerContext, protoContainer, packageProto.functionList, packageProto.propertyList)
    createTypeAliasesStubs(parentStub, outerContext, protoContainer, packageProto.typeAliasList)
}

fun createDeclarationsStubs(
    parentStub: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer,
    functionProtos: List<ProtoBuf.Function>,
    propertyProtos: List<ProtoBuf.Property>,
) {
    for (propertyProto in propertyProtos) {
        if (!shouldSkip(propertyProto.flags, outerContext.nameResolver.getName(propertyProto.name))) {
            PropertyClsStubBuilder(parentStub, outerContext, protoContainer, propertyProto).build()
        }
    }
    for (functionProto in functionProtos) {
        if (!shouldSkip(functionProto.flags, outerContext.nameResolver.getName(functionProto.name))) {
            FunctionClsStubBuilder(parentStub, outerContext, protoContainer, functionProto).build()
        }
    }
}

fun createTypeAliasesStubs(
    parentStub: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer,
    typeAliasesProtos: List<ProtoBuf.TypeAlias>
) {
    for (typeAliasProto in typeAliasesProtos) {
        createTypeAliasStub(parentStub, typeAliasProto, protoContainer, outerContext)
    }
}

fun createConstructorStub(
    parentStub: StubElement<out PsiElement>,
    constructorProto: ProtoBuf.Constructor,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer
) {
    ConstructorClsStubBuilder(parentStub, outerContext, protoContainer, constructorProto).build()
}

private fun shouldSkip(flags: Int, name: Name): Boolean {
    return when (Flags.MEMBER_KIND.get(flags)) {
        MemberKind.FAKE_OVERRIDE, MemberKind.DELEGATION -> true
        //TODO: fix decompiler to use sane criteria
        MemberKind.SYNTHESIZED -> !DataClassResolver.isComponentLike(name)
        else -> false
    }
}

abstract class CallableClsStubBuilder(
    parent: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protected val protoContainer: ProtoContainer,
    private val typeParameters: List<ProtoBuf.TypeParameter>
) {
    protected val c = outerContext.child(typeParameters)
    protected val typeStubBuilder = TypeClsStubBuilder(c)
    private val contextReceiversListStubBuilder = ContextReceiversListStubBuilder(c)
    protected val isTopLevel: Boolean get() = protoContainer is ProtoContainer.Package
    protected val callableStub: StubElement<out PsiElement> by lazy(LazyThreadSafetyMode.NONE) { doCreateCallableStub(parent) }

    fun build() {
        contextReceiversListStubBuilder.createContextReceiverStubs(callableStub, contextReceiverTypes)
        createModifierListStub()
        val typeConstraintListData = typeStubBuilder.createTypeParameterListStub(callableStub, typeParameters)
        createReceiverTypeReferenceStub()
        createValueParameterList()
        createReturnTypeStub()
        typeStubBuilder.createTypeConstraintListStub(callableStub, typeConstraintListData)
    }

    abstract val receiverType: ProtoBuf.Type?
    abstract val receiverAnnotations: List<ClassIdWithTarget>

    abstract val returnType: ProtoBuf.Type?
    abstract val contextReceiverTypes: List<ProtoBuf.Type>

    private fun createReceiverTypeReferenceStub() {
        receiverType?.let {
            typeStubBuilder.createTypeReferenceStub(callableStub, it, this::receiverAnnotations)
        }
    }

    private fun createReturnTypeStub() {
        returnType?.let {
            typeStubBuilder.createTypeReferenceStub(callableStub, it)
        }
    }

    abstract fun createModifierListStub()

    abstract fun createValueParameterList()

    abstract fun doCreateCallableStub(parent: StubElement<out PsiElement>): StubElement<out PsiElement>
}

private class FunctionClsStubBuilder(
    parent: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer,
    private val functionProto: ProtoBuf.Function
) : CallableClsStubBuilder(parent, outerContext, protoContainer, functionProto.typeParameterList) {
    override val receiverType: ProtoBuf.Type?
        get() = functionProto.receiverType(c.typeTable)

    override val receiverAnnotations: List<ClassIdWithTarget>
        get() {
            return c.components.annotationLoader
                .loadExtensionReceiverParameterAnnotations(protoContainer, functionProto, AnnotatedCallableKind.FUNCTION)
                .map { ClassIdWithTarget(it, AnnotationUseSiteTarget.RECEIVER) }
        }

    override val returnType: ProtoBuf.Type
        get() = functionProto.returnType(c.typeTable)

    override val contextReceiverTypes: List<ProtoBuf.Type>
        get() = functionProto.contextReceiverTypes(c.typeTable)

    override fun createValueParameterList() {
        typeStubBuilder.createValueParameterListStub(callableStub, functionProto, functionProto.valueParameterList, protoContainer)
    }

    override fun createModifierListStub() {
        val modalityModifier = if (isTopLevel) listOf() else listOf(MODALITY)
        val modifierListStubImpl = createModifierListStubForDeclaration(
            callableStub, functionProto.flags,
            listOf(VISIBILITY, OPERATOR, INFIX, EXTERNAL_FUN, INLINE, TAILREC, SUSPEND, EXPECT_FUNCTION) + modalityModifier
        )

        // If function is marked as having no annotations, we don't create stubs for it
        if (!Flags.HAS_ANNOTATIONS.get(functionProto.flags)) return

        val annotationIds = c.components.annotationLoader.loadCallableAnnotations(
            protoContainer, functionProto, AnnotatedCallableKind.FUNCTION
        )
        createAnnotationStubs(annotationIds, modifierListStubImpl)
    }

    override fun doCreateCallableStub(parent: StubElement<out PsiElement>): StubElement<out PsiElement> {
        val callableName = c.nameResolver.getName(functionProto.name)

        // Note that arguments passed to stubs here and elsewhere are based on what stabs would be generated based on decompiled code
        // As functions are never decompiled to fun f() = 1 form, hasBlockBody is always true
        // This info is anyway irrelevant for the purposes these stubs are used
        val hasContract = functionProto.hasContract()
        return KotlinFunctionStubImpl(
            parent,
            callableName.ref(),
            isTopLevel,
            c.containerFqName.child(callableName),
            isExtension = functionProto.hasReceiver(),
            hasBlockBody = true,
            hasBody = Flags.MODALITY.get(functionProto.flags) != Modality.ABSTRACT,
            hasTypeParameterListBeforeFunctionName = functionProto.typeParameterList.isNotEmpty(),
            mayHaveContract = hasContract,
            runIf(hasContract) {
                ClsContractBuilder(c, typeStubBuilder).loadContract(functionProto)
            }
        )
    }
}

private class PropertyClsStubBuilder(
    parent: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer,
    private val propertyProto: ProtoBuf.Property
) : CallableClsStubBuilder(parent, outerContext, protoContainer, propertyProto.typeParameterList) {
    private val isVar = Flags.IS_VAR.get(propertyProto.flags)

    override val receiverType: ProtoBuf.Type?
        get() = propertyProto.receiverType(c.typeTable)

    override val receiverAnnotations: List<ClassIdWithTarget>
        get() = c.components.annotationLoader
            .loadExtensionReceiverParameterAnnotations(protoContainer, propertyProto, AnnotatedCallableKind.PROPERTY_GETTER)
            .map { ClassIdWithTarget(it, AnnotationUseSiteTarget.RECEIVER) }

    override val returnType: ProtoBuf.Type
        get() = propertyProto.returnType(c.typeTable)

    override val contextReceiverTypes: List<ProtoBuf.Type>
        get() = propertyProto.contextReceiverTypes(c.typeTable)

    override fun createValueParameterList() {
    }

    override fun createModifierListStub() {
        val constModifier = if (isVar) listOf() else listOf(CONST)
        val modalityModifier = if (isTopLevel) listOf() else listOf(MODALITY)

        val modifierListStubImpl = createModifierListStubForDeclaration(
            callableStub, propertyProto.flags,
            listOf(VISIBILITY, LATEINIT, EXTERNAL_PROPERTY, EXPECT_PROPERTY) + constModifier + modalityModifier
        )

        // If field is marked as having no annotations, we don't create stubs for it
        if (!Flags.HAS_ANNOTATIONS.get(propertyProto.flags)) return

        val propertyAnnotations =
            c.components.annotationLoader.loadCallableAnnotations(protoContainer, propertyProto, AnnotatedCallableKind.PROPERTY)
        val backingFieldAnnotations =
            c.components.annotationLoader.loadPropertyBackingFieldAnnotations(protoContainer, propertyProto)
        val delegateFieldAnnotations =
            c.components.annotationLoader.loadPropertyDelegateFieldAnnotations(protoContainer, propertyProto)
        val allAnnotations =
            propertyAnnotations.map { ClassIdWithTarget(it, null) } +
                    backingFieldAnnotations.map { ClassIdWithTarget(it, AnnotationUseSiteTarget.FIELD) } +
                    delegateFieldAnnotations.map { ClassIdWithTarget(it, AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD) }
        createTargetedAnnotationStubs(allAnnotations, modifierListStubImpl)
    }

    override fun doCreateCallableStub(parent: StubElement<out PsiElement>): StubElement<out PsiElement> {
        val callableName = c.nameResolver.getName(propertyProto.name)

        // Note that arguments passed to stubs here and elsewhere are based on what stabs would be generated based on decompiled code
        // This info is anyway irrelevant for the purposes these stubs are used
        return KotlinPropertyStubImpl(
            parent,
            callableName.ref(),
            isVar,
            isTopLevel,
            hasDelegate = false,
            hasDelegateExpression = false,
            hasInitializer = false,
            isExtension = propertyProto.hasReceiver(),
            hasReturnTypeRef = true,
            fqName = c.containerFqName.child(callableName)
        )
    }
}

private class ConstructorClsStubBuilder(
    parent: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer,
    private val constructorProto: ProtoBuf.Constructor
) : CallableClsStubBuilder(parent, outerContext, protoContainer, emptyList()) {
    override val receiverType: ProtoBuf.Type?
        get() = null

    override val receiverAnnotations: List<ClassIdWithTarget>
        get() = emptyList()

    override val returnType: ProtoBuf.Type?
        get() = null

    override val contextReceiverTypes: List<ProtoBuf.Type>
        get() = emptyList()

    override fun createValueParameterList() {
        typeStubBuilder.createValueParameterListStub(callableStub, constructorProto, constructorProto.valueParameterList, protoContainer)
    }

    override fun createModifierListStub() {
        val modifierListStubImpl = createModifierListStubForDeclaration(callableStub, constructorProto.flags, listOf(VISIBILITY))

        // If constructor is marked as having no annotations, we don't create stubs for it
        if (!Flags.HAS_ANNOTATIONS.get(constructorProto.flags)) return

        val annotationIds = c.components.annotationLoader.loadCallableAnnotations(
            protoContainer, constructorProto, AnnotatedCallableKind.FUNCTION
        )
        createAnnotationStubs(annotationIds, modifierListStubImpl)
    }

    override fun doCreateCallableStub(parent: StubElement<out PsiElement>): StubElement<out PsiElement> {
        val name = (protoContainer as ProtoContainer.Class).classId.shortClassName.ref()
        // Note that arguments passed to stubs here and elsewhere are based on what stabs would be generated based on decompiled code
        // As decompiled code for secondary constructor would be just constructor(args) { /* compiled code */ } every secondary constructor
        // delegated call is not to this (as there is no this keyword) and it has body (while primary does not have one)
        // This info is anyway irrelevant for the purposes these stubs are used
        return if (Flags.IS_SECONDARY.get(constructorProto.flags))
            KotlinConstructorStubImpl(parent, KtStubElementTypes.SECONDARY_CONSTRUCTOR, name, hasBody = true, isDelegatedCallToThis = false)
        else
            KotlinConstructorStubImpl(parent, KtStubElementTypes.PRIMARY_CONSTRUCTOR, name, hasBody = false, isDelegatedCallToThis = false)
    }
}
