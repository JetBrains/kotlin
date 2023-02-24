// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.analysis.decompiler.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.analysis.decompiler.stub.flags.*
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.findKotlinClass
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.MemberKind
import org.jetbrains.kotlin.metadata.ProtoBuf.Modality
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.stubs.ConstantValueKind
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.AnnotatedCallableKind
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.getName

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
    abstract val receiverAnnotations: List<AnnotationWithTarget>

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

    override val receiverAnnotations: List<AnnotationWithTarget>
        get() {
            return c.components.annotationLoader
                .loadExtensionReceiverParameterAnnotations(protoContainer, functionProto, AnnotatedCallableKind.FUNCTION)
                .map { AnnotationWithTarget(it, AnnotationUseSiteTarget.RECEIVER) }
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

        val annotations = c.components.annotationLoader.loadCallableAnnotations(
            protoContainer, functionProto, AnnotatedCallableKind.FUNCTION
        )
        createAnnotationStubs(annotations, modifierListStubImpl)
    }

    override fun doCreateCallableStub(parent: StubElement<out PsiElement>): StubElement<out PsiElement> {
        val callableName = c.nameResolver.getName(functionProto.name)

        // Note that arguments passed to stubs here and elsewhere are based on what stabs would be generated based on decompiled code
        // As functions are never decompiled to fun f() = 1 form, hasBlockBody is always true
        // This info is anyway irrelevant for the purposes these stubs are used
        val functionStub = KotlinFunctionStubImpl(
            parent,
            callableName.ref(),
            isTopLevel,
            c.containerFqName.child(callableName),
            isExtension = functionProto.hasReceiver(),
            hasBlockBody = true,
            hasBody = Flags.MODALITY[functionProto.flags] != Modality.ABSTRACT,
            hasTypeParameterListBeforeFunctionName = functionProto.typeParameterList.isNotEmpty(),
            mayHaveContract = functionProto.hasContract()
        )
        if (functionProto.hasContract()) {
            val contractDeserializer = ClsContractBuilder(typeStubBuilder, c.typeTable)
            contractDeserializer.loadContract(functionProto.contract, functionStub)
        }
        return functionStub
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

    override val receiverAnnotations: List<AnnotationWithTarget>
        get() = c.components.annotationLoader
            .loadExtensionReceiverParameterAnnotations(protoContainer, propertyProto, AnnotatedCallableKind.PROPERTY_GETTER)
            .map { AnnotationWithTarget(it, AnnotationUseSiteTarget.RECEIVER) }

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
            propertyAnnotations.map { AnnotationWithTarget(it, null) } +
                    backingFieldAnnotations.map { AnnotationWithTarget(it, AnnotationUseSiteTarget.FIELD) } +
                    delegateFieldAnnotations.map { AnnotationWithTarget(it, AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD) }
        createTargetedAnnotationStubs(allAnnotations, modifierListStubImpl)
    }

    override fun doCreateCallableStub(parent: StubElement<out PsiElement>): StubElement<out PsiElement> {
        val callableName = c.nameResolver.getName(propertyProto.name)
        val flags = propertyProto.flags
        // Note that arguments passed to stubs here and elsewhere are based on what stabs would be generated based on decompiled code
        // This info is anyway irrelevant for the purposes these stubs are used
        val hasInitializer = Flags.HAS_CONSTANT[flags]
        val propertyStub = KotlinPropertyStubImpl(
            parent,
            callableName.ref(),
            isVar,
            isTopLevel,
            hasDelegate = false,
            hasDelegateExpression = false,
            hasInitializer = hasInitializer,
            isExtension = propertyProto.hasReceiver(),
            hasReturnTypeRef = true,
            fqName = c.containerFqName.child(callableName)
        )

        val defaultAccessorFlags = Flags.getAccessorFlags(
            Flags.HAS_ANNOTATIONS[flags],
            Flags.VISIBILITY[flags],
            Flags.MODALITY[flags],
            false, false, false
        )
        if (Flags.HAS_GETTER[flags]) {
            val hasCustomGetter = propertyProto.hasGetterFlags()
            val getterStub = KotlinPropertyAccessorStubImpl(propertyStub, true, hasCustomGetter, false)
            createModifierListStubForDeclaration(
                getterStub,
                if (hasCustomGetter) propertyProto.getterFlags else defaultAccessorFlags,
                listOf(VISIBILITY, MODALITY, INLINE, EXTERNAL_FUN)
            )
        }

        if (Flags.HAS_SETTER[flags]) {
            val hasCustomSetter = propertyProto.hasSetterFlags()
            val setterStub = KotlinPropertyAccessorStubImpl(propertyStub, false, hasCustomSetter, false)
            createModifierListStubForDeclaration(
                setterStub,
                if (hasCustomSetter) propertyProto.setterFlags else defaultAccessorFlags,
                listOf(VISIBILITY, MODALITY, INLINE, EXTERNAL_FUN)
            )
            if (propertyProto.hasSetterValueParameter()) {
                typeStubBuilder.createValueParameterListStub(
                    setterStub,
                    propertyProto,
                    listOf(propertyProto.setterValueParameter),
                    protoContainer
                )
            }
        }
        if (hasInitializer) {
            val binaryClass = when (val source = getSource()) {
                is JvmPackagePartSource -> {
                    val knownJvmBinaryClass = source.knownJvmBinaryClass
                    val facadeName = knownJvmBinaryClass?.classHeader?.multifileClassName?.takeIf { it.isNotEmpty() }
                    val facadeFqName = facadeName?.let { JvmClassName.byInternalName(it).fqNameForTopLevelClassMaybeWithDollars }
                    val facadeBinaryClass = facadeFqName?.let {
                        c.components.classFinder?.findKotlinClass(ClassId.topLevel(it), c.components.jvmMetadataVersion!!)
                    }
                    facadeBinaryClass ?: knownJvmBinaryClass
                }
                is KotlinJvmBinarySourceElement -> {
                    source.binaryClass
                }
                else -> null
            }
            if (binaryClass != null) {
                binaryClass.visitMembers(object : KotlinJvmBinaryClass.MemberVisitor {
                    override fun visitMethod(name: Name, desc: String): KotlinJvmBinaryClass.MethodAnnotationVisitor? = null

                    override fun visitField(name: Name, desc: String, initializer: Any?): KotlinJvmBinaryClass.AnnotationVisitor? {
                        if (initializer != null) {
                            buildConstantInitializer(initializer, null, desc, propertyStub)
                        }
                        return null
                    }
                }, null)
            } else {
                val value = propertyProto.getExtensionOrNull(BuiltInSerializerProtocol.compileTimeValue)
                if (value != null) {
                    buildConstantInitializer(null, value, value.type.name, propertyStub)
                }
            }
        }
        return propertyStub
    }

    /**
     * [org.jetbrains.kotlin.load.kotlin.AbstractBinaryClassAnnotationLoader.getSpecialCaseContainerClass]
     */
    //special cases when data might be stored in a neighbour class
    private fun getSource(): SourceElement? {
        if (protoContainer is ProtoContainer.Class && protoContainer.kind == ProtoBuf.Class.Kind.COMPANION_OBJECT) {
            val outerClass = protoContainer.outerClass
            if (outerClass != null && (outerClass.kind == ProtoBuf.Class.Kind.CLASS || outerClass.kind == ProtoBuf.Class.Kind.ENUM_CLASS ||
                        JvmProtoBufUtil.isMovedFromInterfaceCompanion(propertyProto) &&
                        (outerClass.kind == ProtoBuf.Class.Kind.INTERFACE || outerClass.kind == ProtoBuf.Class.Kind.ANNOTATION_CLASS))
            ) {
                return outerClass.source
            }
        }
        return protoContainer.source
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

    override val receiverAnnotations: List<AnnotationWithTarget>
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
fun buildConstantInitializer(
    value1: Any?, builtInValue: ProtoBuf.Annotation.Argument.Value?, constKind: String, parent: KotlinStubBaseImpl<*>
): KotlinStubBaseImpl<*>? {
    return when (constKind) {
        "BYTE", "B", "SHORT", "S", "LONG", "J", "INT", "I" -> KotlinConstantExpressionStubImpl(
            parent,
            KtStubElementTypes.INTEGER_CONSTANT,
            ConstantValueKind.INTEGER_CONSTANT,
            StringRef.fromString(((value1 ?: builtInValue?.intValue) as Number).toString())
        )
        "CHAR", "C" -> KotlinConstantExpressionStubImpl(
            parent,
            KtStubElementTypes.CHARACTER_CONSTANT,
            ConstantValueKind.CHARACTER_CONSTANT,
            StringRef.fromString(((value1 ?: builtInValue?.intValue) as Number).toInt().toChar().toString())
        )
        "FLOAT", "F" -> KotlinConstantExpressionStubImpl(
            parent,
            KtStubElementTypes.FLOAT_CONSTANT,
            ConstantValueKind.FLOAT_CONSTANT,
            StringRef.fromString(((value1 ?: builtInValue?.floatValue) as Float).toString())
        )
        "DOUBLE", "D" -> KotlinConstantExpressionStubImpl(
            parent,
            KtStubElementTypes.FLOAT_CONSTANT,
            ConstantValueKind.FLOAT_CONSTANT,
            StringRef.fromString(((value1 ?: builtInValue?.doubleValue) as Double).toString())
        )
        "BOOLEAN", "Z" -> KotlinConstantExpressionStubImpl(
            parent,
            KtStubElementTypes.BOOLEAN_CONSTANT,
            ConstantValueKind.BOOLEAN_CONSTANT,
            StringRef.fromString(((value1 ?: builtInValue?.intValue) != 0).toString())
        )
        "STRING", "Ljava/lang/String;" -> {
            val text = (value1 ?: builtInValue?.stringValue) as String
            val stringTemplate = KotlinPlaceHolderStubImpl<KtStringTemplateExpression>(
                parent, KtStubElementTypes.STRING_TEMPLATE
            )
            return KotlinPlaceHolderWithTextStubImpl<KtSimpleNameStringTemplateEntry>(
                stringTemplate,
                KtStubElementTypes.SHORT_STRING_TEMPLATE_ENTRY,
                text
            )
        }
        else -> null
    }
}
