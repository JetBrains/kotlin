/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.deserialization.AbstractAnnotationDeserializer
import org.jetbrains.kotlin.fir.deserialization.ProtoContainer
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.load.kotlin.AbstractBinaryClassAnnotationAndConstantLoader
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.MemberSignature
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.resolve.constants.ClassLiteralValue
import org.jetbrains.kotlin.serialization.deserialization.AnnotatedCallableKind
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.compact
import java.util.*

class JvmBinaryAnnotationDeserializer(
    session: FirSession
) : AbstractAnnotationDeserializer(session) {
    private val cache: MutableMap<KotlinJvmBinaryClass, Storage> = mutableMapOf()

    private class Storage(
        val memberAnnotations: Map<MemberSignature, List<FirAnnotationCall>>,
        val propertyConstants: Map<MemberSignature, FirExpression>
    )

    private val ProtoContainer.kotlinJvmBinaryClass: KotlinJvmBinaryClass?
        get() = (sourceElement as? JvmPackagePartSource)?.knownJvmBinaryClass

    override fun loadTypeAnnotations(
        containingDeclaration: ProtoContainer,
        typeProto: ProtoBuf.Type,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotationCall> {
        val annotations = typeProto.getExtension(JvmProtoBuf.typeAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver) }
    }

    override fun loadFunctionAnnotations(
        containingDeclaration: ProtoContainer,
        functionProto: ProtoBuf.Function,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotationCall> {
        val kotlinClass = containingDeclaration.kotlinJvmBinaryClass ?: return emptyList()
        val signature = getCallableSignature(functionProto, nameResolver, typeTable, AnnotatedCallableKind.FUNCTION) ?: return emptyList()
        val storage = loadClassFileContent(kotlinClass)
        return storage.memberAnnotations[signature] ?: emptyList()
    }

    override fun loadPropertyAnnotations(
        containingDeclaration: ProtoContainer,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotationCall> {
        val kotlinClass = containingDeclaration.kotlinJvmBinaryClass ?: return emptyList()
        val signature = getPropertySignature(propertyProto, nameResolver, typeTable) ?: return emptyList()
        val storage = loadClassFileContent(kotlinClass)
        return storage.memberAnnotations[signature] ?: emptyList()
    }

    override fun loadConstructorAnnotations(
        containingDeclaration: ProtoContainer,
        constructorProto: ProtoBuf.Constructor,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotationCall> {
        val kotlinClass = containingDeclaration.kotlinJvmBinaryClass ?: return emptyList()
        val signature = getCallableSignature(constructorProto, nameResolver, typeTable, AnnotatedCallableKind.FUNCTION) ?: return emptyList()
        val storage = loadClassFileContent(kotlinClass)
        return storage.memberAnnotations[signature] ?: emptyList()
    }

    private fun getCallableSignature(
        proto: MessageLite,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: AnnotatedCallableKind,
        requireHasFieldFlagForField: Boolean = false
    ): MemberSignature? {
        return when (proto) {
            is ProtoBuf.Constructor -> {
                MemberSignature.fromJvmMemberSignature(
                    JvmProtoBufUtil.getJvmConstructorSignature(proto, nameResolver, typeTable) ?: return null
                )
            }
            is ProtoBuf.Function -> {
                MemberSignature.fromJvmMemberSignature(JvmProtoBufUtil.getJvmMethodSignature(proto, nameResolver, typeTable) ?: return null)
            }
            is ProtoBuf.Property -> {
                val signature = proto.getExtensionOrNull(JvmProtoBuf.propertySignature) ?: return null
                when (kind) {
                    AnnotatedCallableKind.PROPERTY_GETTER ->
                        if (signature.hasGetter()) MemberSignature.fromMethod(nameResolver, signature.getter) else null
                    AnnotatedCallableKind.PROPERTY_SETTER ->
                        if (signature.hasSetter()) MemberSignature.fromMethod(nameResolver, signature.setter) else null
                    AnnotatedCallableKind.PROPERTY ->
                        getPropertySignature(proto, nameResolver, typeTable, true, true, requireHasFieldFlagForField)
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun getPropertySignature(
        proto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        field: Boolean = false,
        synthetic: Boolean = false,
        requireHasFieldFlagForField: Boolean = true
    ): MemberSignature? {
        val signature = proto.getExtensionOrNull(JvmProtoBuf.propertySignature) ?: return null

        if (field) {
            val fieldSignature =
                JvmProtoBufUtil.getJvmFieldSignature(proto, nameResolver, typeTable, requireHasFieldFlagForField) ?: return null
            return MemberSignature.fromJvmMemberSignature(fieldSignature)
        } else if (synthetic && signature.hasSyntheticMethod()) {
            return MemberSignature.fromMethod(nameResolver, signature.syntheticMethod)
        }

        return null
    }


    private fun loadClassFileContent(kotlinClass: KotlinJvmBinaryClass): Storage {
        cache[kotlinClass]?.let { return it }

        val memberAnnotations = HashMap<MemberSignature, MutableList<FirAnnotationCall>>()
        val propertyConstants = HashMap<MemberSignature, FirExpression>()

        kotlinClass.visitMembers(object : KotlinJvmBinaryClass.MemberVisitor {
            override fun visitMethod(name: Name, desc: String): KotlinJvmBinaryClass.MethodAnnotationVisitor? {
                return AnnotationVisitorForMethod(MemberSignature.fromMethodNameAndDesc(name.asString(), desc))
            }

            override fun visitField(name: Name, desc: String, initializer: Any?): KotlinJvmBinaryClass.AnnotationVisitor? {
                val signature = MemberSignature.fromFieldNameAndDesc(name.asString(), desc)

                if (initializer != null) {
                    val constant = loadConstant(desc, initializer)
                    if (constant != null) {
                        propertyConstants[signature] = constant
                    }
                }
                return MemberAnnotationVisitor(signature)
            }

            inner class AnnotationVisitorForMethod(signature: MemberSignature) : MemberAnnotationVisitor(signature),
                KotlinJvmBinaryClass.MethodAnnotationVisitor {

                override fun visitParameterAnnotation(
                    index: Int, classId: ClassId, source: SourceElement
                ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(signature, index)
                    var result = memberAnnotations[paramSignature]
                    if (result == null) {
                        result = ArrayList()
                        memberAnnotations[paramSignature] = result
                    }
                    return loadAnnotationIfNotSpecial(classId, source, result)
                }
            }

            open inner class MemberAnnotationVisitor(protected val signature: MemberSignature) : KotlinJvmBinaryClass.AnnotationVisitor {
                private val result = ArrayList<FirAnnotationCall>()

                override fun visitAnnotation(classId: ClassId, source: SourceElement): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    return loadAnnotationIfNotSpecial(classId, source, result)
                }

                override fun visitEnd() {
                    if (result.isNotEmpty()) {
                        memberAnnotations[signature] = result
                    }
                }
            }
        }, getCachedFileContent(kotlinClass))

        return Storage(memberAnnotations, propertyConstants).also {
            cache[kotlinClass] = it
        }
    }

    fun getCachedFileContent(kotlinClass: KotlinJvmBinaryClass): ByteArray? = null

    private fun loadConstant(desc: String, initializer: Any): FirExpression? {
        val normalizedValue: Any = if (desc in "ZBCS") {
            val intValue = initializer as Int
            when (desc) {
                "Z" -> intValue != 0
                "B" -> intValue.toByte()
                "C" -> intValue.toChar()
                "S" -> intValue.toShort()
                else -> throw AssertionError(desc)
            }
        } else {
            initializer
        }

        return ConstantValueFactory.createConstantValue(normalizedValue)
    }

    private fun loadAnnotationIfNotSpecial(
        annotationClassId: ClassId,
        source: SourceElement,
        result: MutableList<FirAnnotationCall>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        if (annotationClassId in AbstractBinaryClassAnnotationAndConstantLoader.SPECIAL_ANNOTATIONS) return null

        return loadAnnotation(annotationClassId, result)
    }

    fun loadClass(classId: ClassId): FirClassSymbol<*>? {
        return session.firSymbolProvider.getClassLikeSymbolByFqName(classId) as? FirClassSymbol<*>
    }

    private fun ClassId.toConeKotlinType(): ConeKotlinType = ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(this), emptyArray(), isNullable = false)

    fun loadAnnotation(
        annotationClassId: ClassId,
        result: MutableList<FirAnnotationCall>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        val annotationClass = loadClass(annotationClassId) ?: return null
        val annotationConstructor = annotationClass.fir.declarations.firstIsInstanceOrNull<FirConstructor>() ?: return null

        val builder = FirAnnotationCallBuilder().apply {
            annotationTypeRef = buildResolvedTypeRef {
                type = annotationClassId.toConeKotlinType()
            }
        }

        return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor {
            private val arguments = HashMap<Name, FirExpression>()

            override fun visit(name: Name?, value: Any?) {
                if (name != null) {
                    arguments[name] = createConstant(name, value)
                }
            }

            override fun visitClassLiteral(name: Name, value: ClassLiteralValue) {
                arguments[name] = buildClassLiteral(value)
            }

            override fun visitEnum(name: Name, enumClassId: ClassId, enumEntryName: Name) {
                arguments[name] = buildEnum(enumClassId, enumEntryName)
            }

            private fun buildClassLiteral(value: ClassLiteralValue): FirExpression {
                return buildGetClassCall {
                    arguments += buildResolvedQualifier {

                    }
                }
            }

            private fun buildEnum(
                enumClassId: ClassId,
                enumEntryName: Name
            ): FirExpression {
                return buildQualifiedAccessExpression {
                    typeRef = buildResolvedTypeRef {
                        type = enumClassId.toConeKotlinType()
                    }
                    calleeReference = buildResolvedNamedReference {
                        name = enumEntryName
                        resolvedSymbol = loadClass(enumClassId)!!
                    }
                }
            }

            override fun visitArray(name: Name): KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor? {
                return object : KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor {
                    private val elements = ArrayList<FirExpression>()

                    override fun visit(value: Any?) {
                        elements.add(createConstant(name, value))
                    }

                    override fun visitEnum(enumClassId: ClassId, enumEntryName: Name) {
                        elements.add(buildEnum(enumClassId, enumEntryName))
                    }

                    override fun visitClassLiteral(value: ClassLiteralValue) {
                        elements.add(buildClassLiteral(value))
                    }

                    override fun visitEnd() {
                        val parameter = annotationConstructor.valueParameters.firstOrNull { it.name == name }
                        if (parameter != null) {
                            arguments[name] = ConstantValueFactory.createArrayValue(elements.compact())
                        }
                    }
                }
            }

            override fun visitAnnotation(name: Name, classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                val list = ArrayList<FirAnnotationCall>()
                val visitor = loadAnnotation(classId, list)!!
                return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
                    override fun visitEnd() {
                        visitor.visitEnd()
                        arguments[name] = list.single()
                    }
                }
            }

            override fun visitEnd() {
                result += builder.build()
            }

            private fun createConstant(name: Name?, value: Any?): FirExpression {
                return ConstantValueFactory.createConstantValue(value)
                    ?: buildErrorExpression {
                        diagnostic = FirSimpleDiagnostic("Unsupported annotation argument: $name")
                    }
            }
        }
    }

}