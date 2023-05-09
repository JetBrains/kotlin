// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.analysis.decompiler.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.analysis.decompiler.stub.flags.*
import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.MemberKind
import org.jetbrains.kotlin.metadata.ProtoBuf.Modality
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.resolve.constants.ClassLiteralValue
import org.jetbrains.kotlin.serialization.deserialization.AnnotatedCallableKind
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf

const val COMPILED_DEFAULT_INITIALIZER = "COMPILED_CODE"

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
        MemberKind.SYNTHESIZED -> !DataClassResolver.isComponentLike(name) && name !in listOf(
            OperatorNameConventions.EQUALS,
            StandardNames.HASHCODE_NAME,
            OperatorNameConventions.TO_STRING
        )
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
        createCallableSpecialParts()
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

    protected open fun createCallableSpecialParts() {}
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
        val initializer = calcInitializer()

        // Note that arguments passed to stubs here and elsewhere are based on what stabs would be generated based on decompiled code
        // This info is anyway irrelevant for the purposes these stubs are used
        return KotlinPropertyStubImpl(
            parent,
            callableName.ref(),
            isVar,
            isTopLevel,
            hasDelegate = false,
            hasDelegateExpression = false,
            hasInitializer = initializer != null,
            isExtension = propertyProto.hasReceiver(),
            hasReturnTypeRef = true,
            fqName = c.containerFqName.child(callableName),
            initializer
        )
    }

    override fun createCallableSpecialParts() {
        if ((callableStub as KotlinPropertyStub).hasInitializer()) {
            KotlinNameReferenceExpressionStubImpl(callableStub, StringRef.fromString(COMPILED_DEFAULT_INITIALIZER))
        }
        val flags = propertyProto.flags
        if (Flags.HAS_GETTER[flags] && propertyProto.hasGetterFlags()) {
            val getterFlags = propertyProto.getterFlags
            if (Flags.IS_NOT_DEFAULT.get(getterFlags)) {
                createModifierListAndAnnotationStubsForAccessor(
                    KotlinPropertyAccessorStubImpl(callableStub, true, false, true),
                    flags = getterFlags,
                    callableKind = AnnotatedCallableKind.PROPERTY_GETTER
                )
            }
        }

        if (Flags.HAS_SETTER[flags] && propertyProto.hasSetterFlags()) {
            val setterFlags = propertyProto.setterFlags
            if (Flags.IS_NOT_DEFAULT.get(setterFlags)) {
                val setterStub = KotlinPropertyAccessorStubImpl(callableStub, false, true, true)
                createModifierListAndAnnotationStubsForAccessor(
                    setterStub,
                    flags = setterFlags,
                    callableKind = AnnotatedCallableKind.PROPERTY_SETTER
                )
                if (propertyProto.hasSetterValueParameter()) {
                    typeStubBuilder.createValueParameterListStub(
                        setterStub,
                        propertyProto,
                        listOf(propertyProto.setterValueParameter),
                        protoContainer,
                        AnnotatedCallableKind.PROPERTY_SETTER
                    )
                }
            }
        }
    }

    private fun createModifierListAndAnnotationStubsForAccessor(
        accessorStub: KotlinPropertyAccessorStubImpl,
        flags: Int,
        callableKind: AnnotatedCallableKind
    ) {
        val modifierList = createModifierListStubForDeclaration(
            accessorStub,
            flags,
            listOf(VISIBILITY, MODALITY, INLINE_ACCESSOR, EXTERNAL_ACCESSOR)
        )
        if (Flags.HAS_ANNOTATIONS.get(flags)) {
            val annotationIds = c.components.annotationLoader.loadCallableAnnotations(
                protoContainer,
                propertyProto,
                callableKind
            )
            createAnnotationStubs(annotationIds, modifierList)
        }
    }

    private fun calcInitializer(): ConstantValue<*>? {
        val classFinder = c.components.classFinder
        val containerClass =
            if (classFinder != null) getSpecialCaseContainerClass(classFinder, c.components.jvmMetadataVersion!!) else null
        val source = protoContainer.source
        val binaryClass = containerClass ?: (source as? KotlinJvmBinarySourceElement)?.binaryClass
        var constantInitializer: ConstantValue<*>? = null
        if (binaryClass != null) {
            val callableName = c.nameResolver.getName(propertyProto.name)
            binaryClass.visitMembers(object : KotlinJvmBinaryClass.MemberVisitor {
                private val getterName = lazy(LazyThreadSafetyMode.NONE) {
                    val signature = propertyProto.getExtensionOrNull(JvmProtoBuf.propertySignature) ?: return@lazy null
                    c.nameResolver.getName(signature.getter.name)
                }

                override fun visitMethod(name: Name, desc: String): KotlinJvmBinaryClass.MethodAnnotationVisitor? {
                    if (protoContainer is ProtoContainer.Class && protoContainer.kind == ProtoBuf.Class.Kind.ANNOTATION_CLASS && getterName.value == name) {
                        return object : KotlinJvmBinaryClass.MethodAnnotationVisitor {
                            override fun visitParameterAnnotation(
                                index: Int,
                                classId: ClassId,
                                source: SourceElement
                            ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? = null

                            override fun visitAnnotationMemberDefaultValue(): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
                                return object : AnnotationMemberDefaultValueVisitor() {
                                    override fun visitEnd() {
                                        constantInitializer = args.values.firstOrNull()
                                    }
                                }
                            }

                            override fun visitAnnotation(
                                classId: ClassId,
                                source: SourceElement
                            ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? = null

                            override fun visitEnd() {}
                        }
                    }
                    return null
                }

                override fun visitField(name: Name, desc: String, initializer: Any?): KotlinJvmBinaryClass.AnnotationVisitor? {
                    if (initializer != null && name == callableName) {
                        constantInitializer = createConstantValue(initializer)
                    }
                    return null
                }
            }, null)
        } else {
            val value = propertyProto.getExtensionOrNull(c.components.serializationProtocol.compileTimeValue)
            if (value != null) {
                constantInitializer = createConstantValue(value, c.nameResolver)
            }
        }
        return constantInitializer
    }

    private fun getSpecialCaseContainerClass(
        classFinder: KotlinClassFinder,
        jvmMetadataVersion: JvmMetadataVersion
    ): KotlinJvmBinaryClass? {
        return AbstractBinaryClassAnnotationLoader.getSpecialCaseContainerClass(
            container = protoContainer,
            property = true,
            field = true,
            isConst = Flags.IS_CONST.get(propertyProto.flags),
            isMovedFromInterfaceCompanion = JvmProtoBufUtil.isMovedFromInterfaceCompanion(propertyProto),
            kotlinClassFinder = classFinder,
            jvmMetadataVersion = jvmMetadataVersion
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

open class AnnotationMemberDefaultValueVisitor : KotlinJvmBinaryClass.AnnotationArgumentVisitor {
    protected val args = mutableMapOf<Name, ConstantValue<*>>()

    private fun nameOrSpecial(name: Name?): Name {
        return name ?: Name.special("<no_name>")
    }

    override fun visit(name: Name?, value: Any?) {
        val constantValue = createConstantValue(value)
        args[nameOrSpecial(name)] = constantValue
    }

    override fun visitClassLiteral(name: Name?, value: ClassLiteralValue) {
        args[nameOrSpecial(name)] = createConstantValue(KClassData(value.classId, value.arrayNestedness))
    }

    override fun visitEnum(name: Name?, enumClassId: ClassId, enumEntryName: Name) {
        args[nameOrSpecial(name)] = createConstantValue(EnumData(enumClassId, enumEntryName))
    }

    override fun visitAnnotation(
        name: Name?,
        classId: ClassId
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        val visitor = AnnotationMemberDefaultValueVisitor()
        return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
            override fun visitEnd() {
                args[nameOrSpecial(name)] = createConstantValue(AnnotationData(classId, visitor.args))
            }
        }
    }

    override fun visitArray(name: Name?): KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor? {
        return object : KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor {
            private val elements = mutableListOf<Any>()

            override fun visit(value: Any?) {
                elements.addIfNotNull(value)
            }

            override fun visitEnum(enumClassId: ClassId, enumEntryName: Name) {
                elements.add(EnumData(enumClassId, enumEntryName))
            }

            override fun visitClassLiteral(value: ClassLiteralValue) {
                elements.add(KClassData(value.classId, value.arrayNestedness))
            }

            override fun visitAnnotation(classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
                val visitor = AnnotationMemberDefaultValueVisitor()
                return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
                    override fun visitEnd() {
                        elements.addIfNotNull(AnnotationData(classId, visitor.args))
                    }
                }
            }

            override fun visitEnd() {
                args[nameOrSpecial(name)] = createConstantValue(elements.toTypedArray())
            }
        }
    }

    override fun visitEnd() {}
}