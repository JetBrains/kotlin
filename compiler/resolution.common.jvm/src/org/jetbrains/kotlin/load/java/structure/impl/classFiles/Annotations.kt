/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.load.java.structure.impl.classFiles

import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaAnnotation.Companion.computeTargetType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.*
import java.lang.reflect.Array

internal class AnnotationsCollectorFieldVisitor(
    private val field: BinaryJavaField,
    private val context: ClassifierResolutionContext,
    private val signatureParser: BinaryClassSignatureParser,
) : FieldVisitor(ASM_API_VERSION_FOR_CLASS_READING) {
    override fun visitAnnotation(desc: String, visible: Boolean) =
        BinaryJavaAnnotation.addAnnotation(field, desc, context, signatureParser)

    override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, desc: String, visible: Boolean): AnnotationVisitor? {
        val typeReference = TypeReference(typeRef)

        if (typePath != null) {
            val translatedPath = translatePath(typePath)

            when (typeReference.sort) {
                TypeReference.FIELD -> {
                    val targetType = computeTargetType(field.type, translatedPath)
                    return BinaryJavaAnnotation.addAnnotation(targetType as JavaPlainType, desc, context, signatureParser)
                }
            }
        }

        return when (typeReference.sort) {
            TypeReference.FIELD -> BinaryJavaAnnotation.addAnnotation(field.type as JavaPlainType, desc, context, signatureParser)
            else -> null
        }
    }
}

internal class AnnotationsAndParameterCollectorMethodVisitor(
    private val member: BinaryJavaMethodBase,
    private val context: ClassifierResolutionContext,
    private val signatureParser: BinaryClassSignatureParser,
    private val parametersToSkipNumber: Int,
    private val parametersCountInMethodDesc: Int
) : MethodVisitor(ASM_API_VERSION_FOR_CLASS_READING) {
    private var parameterIndex = 0

    private var visibleAnnotableParameterCount = parametersCountInMethodDesc
    private var invisibleAnnotableParameterCount = parametersCountInMethodDesc

    override fun visitAnnotationDefault(): AnnotationVisitor? =
        BinaryJavaAnnotationVisitor(context, signatureParser) {
            member.safeAs<BinaryJavaMethod>()?.annotationParameterDefaultValue = it
        }

    override fun visitParameter(name: String?, access: Int) {
        if (name != null) {
            val index = parameterIndex - parametersToSkipNumber
            if (index >= 0) {
                val parameter = member.valueParameters.getOrNull(index) ?: error(
                    "No parameter with index $parameterIndex-$parametersToSkipNumber (name=$name access=$access) " +
                            "in method ${member.containingClass.fqName}.${member.name}"
                )
                parameter.updateName(Name.identifier(name))
            }
        }
        parameterIndex++
    }

    override fun visitAnnotation(desc: String, visible: Boolean) =
        BinaryJavaAnnotation.addAnnotation(member, desc, context, signatureParser)

    @Suppress("NOTHING_TO_OVERRIDE")
    override fun visitAnnotableParameterCount(parameterCount: Int, visible: Boolean) {
        if (visible) {
            visibleAnnotableParameterCount = parameterCount
        } else {
            invisibleAnnotableParameterCount = parameterCount
        }
    }

    override fun visitParameterAnnotation(parameter: Int, desc: String, visible: Boolean): AnnotationVisitor? {
        val absoluteParameterIndex =
            parameter + parametersCountInMethodDesc - if (visible) visibleAnnotableParameterCount else invisibleAnnotableParameterCount
        val index = absoluteParameterIndex - parametersToSkipNumber
        if (index < 0) return null

        return BinaryJavaAnnotation.addAnnotation(member.valueParameters[index], desc, context, signatureParser)
    }

    override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, desc: String, visible: Boolean): AnnotationVisitor? {
        val typeReference = TypeReference(typeRef)

        if (typePath != null) {
            val baseType = when (typeReference.sort) {
                TypeReference.METHOD_RETURN -> member.safeAs<BinaryJavaMethod>()?.returnType
                TypeReference.METHOD_FORMAL_PARAMETER -> member.valueParameters[typeReference.formalParameterIndex].type
                TypeReference.METHOD_TYPE_PARAMETER_BOUND ->
                    BinaryJavaAnnotation.computeTypeParameterBound(member.typeParameters, typeReference)
                else -> null
            } ?: return null

            return BinaryJavaAnnotation.addAnnotation(
                computeTargetType(baseType, translatePath(typePath)) as JavaPlainType, desc, context, signatureParser
            )
        }

        val targetType = when (typeReference.sort) {
            TypeReference.METHOD_RETURN -> (member as? BinaryJavaMethod)?.returnType as JavaPlainType
            TypeReference.METHOD_TYPE_PARAMETER -> member.typeParameters[typeReference.typeParameterIndex] as BinaryJavaTypeParameter
            TypeReference.METHOD_FORMAL_PARAMETER -> member.valueParameters[typeReference.formalParameterIndex].type as JavaPlainType
            TypeReference.METHOD_TYPE_PARAMETER_BOUND -> BinaryJavaAnnotation.computeTypeParameterBound(member.typeParameters, typeReference) as JavaPlainType
            else -> null
        } ?: return null

        return BinaryJavaAnnotation.addAnnotation(targetType, desc, context, signatureParser)
    }

    enum class PathElementType { ARRAY_ELEMENT, WILDCARD_BOUND, ENCLOSING_CLASS, TYPE_ARGUMENT }
}

class BinaryJavaAnnotation private constructor(
    desc: String,
    private val context: ClassifierResolutionContext,
    override val arguments: Collection<JavaAnnotationArgument>
) : JavaAnnotation {

    companion object {

        fun createAnnotationAndVisitor(
            desc: String,
            context: ClassifierResolutionContext,
            signatureParser: BinaryClassSignatureParser
        ): Pair<JavaAnnotation, AnnotationVisitor> {
            val arguments = mutableListOf<JavaAnnotationArgument>()
            val annotation = BinaryJavaAnnotation(desc, context, arguments)

            return annotation to BinaryJavaAnnotationVisitor(context, signatureParser, arguments)
        }

        fun addAnnotation(
            annotationOwner: MutableJavaAnnotationOwner,
            desc: String,
            context: ClassifierResolutionContext,
            signatureParser: BinaryClassSignatureParser
        ): AnnotationVisitor {
            val (javaAnnotation, annotationVisitor) = createAnnotationAndVisitor(desc, context, signatureParser)
            annotationOwner.annotations.add(javaAnnotation)
            return annotationVisitor
        }

        internal fun translatePath(path: TypePath): List<Pair<AnnotationsAndParameterCollectorMethodVisitor.PathElementType, Int?>> {
            val length = path.length
            val list = mutableListOf<Pair<AnnotationsAndParameterCollectorMethodVisitor.PathElementType, Int?>>()
            for (i in 0 until length) {
                when (path.getStep(i)) {
                    TypePath.INNER_TYPE -> {
                        continue
                    }
                    TypePath.ARRAY_ELEMENT -> {
                        list.add(AnnotationsAndParameterCollectorMethodVisitor.PathElementType.ARRAY_ELEMENT to null)
                    }
                    TypePath.WILDCARD_BOUND -> {
                        list.add(AnnotationsAndParameterCollectorMethodVisitor.PathElementType.WILDCARD_BOUND to null)
                    }
                    TypePath.TYPE_ARGUMENT -> {
                        list.add(AnnotationsAndParameterCollectorMethodVisitor.PathElementType.TYPE_ARGUMENT to path.getStepArgument(i))
                    }
                }
            }
            return list
        }

        internal fun computeTargetType(
            baseType: JavaType,
            typePath: List<Pair<AnnotationsAndParameterCollectorMethodVisitor.PathElementType, Int?>>
        ): JavaType {
            var targetType = baseType

            for (element in typePath) {
                when (element.first) {
                    AnnotationsAndParameterCollectorMethodVisitor.PathElementType.TYPE_ARGUMENT -> {
                        if (targetType is JavaClassifierType) {
                            targetType = targetType.typeArguments[element.second!!]!!
                        }
                    }
                    AnnotationsAndParameterCollectorMethodVisitor.PathElementType.WILDCARD_BOUND -> {
                        if (targetType is JavaWildcardType) {
                            targetType = targetType.bound!!
                        }
                    }
                    AnnotationsAndParameterCollectorMethodVisitor.PathElementType.ARRAY_ELEMENT -> {
                        if (targetType is JavaArrayType) {
                            targetType = targetType.componentType
                        }
                    }
                }
            }

            return targetType
        }

        internal fun computeTypeParameterBound(
            typeParameters: List<JavaTypeParameter>,
            typeReference: TypeReference
        ): JavaClassifierType {
            val typeParameter = typeParameters[typeReference.typeParameterIndex] as BinaryJavaTypeParameter
            val boundIndex =
                if (typeParameter.hasImplicitObjectClassBound) typeReference.typeParameterBoundIndex - 1 else typeReference.typeParameterBoundIndex

            return typeParameters[typeReference.typeParameterIndex].upperBounds.toList()[boundIndex]
        }
    }

    private val classifierResolutionResult by lazy(LazyThreadSafetyMode.NONE) {
        context.resolveByInternalName(Type.getType(desc).internalName)
    }

    override val classId: ClassId
        get() = classifierResolutionResult.classifier.safeAs<JavaClass>()?.classId
            ?: ClassId.topLevel(FqName(classifierResolutionResult.qualifiedName))

    override fun resolve() = classifierResolutionResult.classifier as? JavaClass
}

class BinaryJavaAnnotationVisitor(
    private val context: ClassifierResolutionContext,
    private val signatureParser: BinaryClassSignatureParser,
    private val sink: (JavaAnnotationArgument) -> Unit
) : AnnotationVisitor(ASM_API_VERSION_FOR_CLASS_READING) {
    constructor(
        context: ClassifierResolutionContext,
        signatureParser: BinaryClassSignatureParser,
        arguments: MutableCollection<JavaAnnotationArgument>
    ) : this(context, signatureParser, { arguments.add(it) })

    private fun addArgument(argument: JavaAnnotationArgument?) {
        if (argument != null) {
            sink(argument)
        }
    }

    override fun visitAnnotation(name: String?, desc: String): AnnotationVisitor {
        val (annotation, visitor) = BinaryJavaAnnotation.createAnnotationAndVisitor(desc, context, signatureParser)

        sink(PlainJavaAnnotationAsAnnotationArgument(name, annotation))

        return visitor
    }

    override fun visitEnum(name: String?, desc: String, value: String) {
        val enumClassId = context.mapInternalNameToClassId(Type.getType(desc).internalName)
        addArgument(PlainJavaEnumValueAnnotationArgument(name, enumClassId, value))
    }

    override fun visit(name: String?, value: Any?) {
        addArgument(convertConstValue(name, value))
    }

    private fun convertConstValue(name: String?, value: Any?): JavaAnnotationArgument? {
        return when (value) {
            is Byte, is Boolean, is Char, is Short, is Int, is Long, is Float, is Double, is String ->
                PlainJavaLiteralAnnotationArgument(name, value)
            is Type -> PlainJavaClassObjectAnnotationArgument(name, value, signatureParser, context)
            else -> value?.takeIf { it.javaClass.isArray }?.let { array ->
                val arguments = (0 until Array.getLength(array)).mapNotNull { index ->
                    convertConstValue(name = null, value = Array.get(array, index))
                }

                PlainJavaArrayAnnotationArgument(name, arguments)
            }
        }
    }

    override fun visitArray(name: String?): AnnotationVisitor {
        val result = mutableListOf<JavaAnnotationArgument>()
        addArgument(PlainJavaArrayAnnotationArgument(name, result))

        return BinaryJavaAnnotationVisitor(context, signatureParser, result)
    }
}

abstract class PlainJavaAnnotationArgument(name: String?) : JavaAnnotationArgument {
    override val name: Name? = name?.takeIf(Name::isValidIdentifier)?.let(Name::identifier)
}

class PlainJavaLiteralAnnotationArgument(
    name: String?,
    override val value: Any?
) : PlainJavaAnnotationArgument(name), JavaLiteralAnnotationArgument

class PlainJavaClassObjectAnnotationArgument(
    name: String?,
    private val type: Type,
    private val signatureParser: BinaryClassSignatureParser,
    private val context: ClassifierResolutionContext
) : PlainJavaAnnotationArgument(name), JavaClassObjectAnnotationArgument {
    override fun getReferencedType() = signatureParser.mapAsmType(type, context)
}

class PlainJavaArrayAnnotationArgument(
    name: String?,
    private val elements: List<JavaAnnotationArgument>
) : PlainJavaAnnotationArgument(name), JavaArrayAnnotationArgument {
    override fun getElements(): List<JavaAnnotationArgument> = elements
}

class PlainJavaAnnotationAsAnnotationArgument(
    name: String?,
    private val annotation: JavaAnnotation
) : PlainJavaAnnotationArgument(name), JavaAnnotationAsAnnotationArgument {
    override fun getAnnotation() = annotation
}

class PlainJavaEnumValueAnnotationArgument(
    name: String?,
    override val enumClassId: ClassId,
    entryName: String
) : PlainJavaAnnotationArgument(name), JavaEnumValueAnnotationArgument {
    override val entryName = Name.identifier(entryName)
}
