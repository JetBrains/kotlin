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
import org.jetbrains.org.objectweb.asm.*
import java.lang.reflect.Array

internal class AnnotationsCollectorFieldVisitor(
    private val field: BinaryJavaField,
    private val context: ClassifierResolutionContext,
    private val signatureParser: BinaryClassSignatureParser,
) : FieldVisitor(ASM_API_VERSION_FOR_CLASS_READING) {
    override fun visitAnnotation(desc: String, visible: Boolean) =
        BinaryJavaAnnotation.addAnnotation(field, desc, context, signatureParser)

    override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String?, visible: Boolean): AnnotationVisitor? {
        if (descriptor == null) return null

        val typeReference = TypeReference(typeRef)
        val targetType = if (typePath != null) computeTargetType(field.type, typePath) else field.type

        if (targetType !is MutableJavaAnnotationOwner) return null

        return when (typeReference.sort) {
            TypeReference.FIELD -> BinaryJavaAnnotation.addAnnotation(
                targetType, descriptor, context, signatureParser, isFreshlySupportedAnnotation = true
            )
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

    val freshlySupportedPositions = setOf(TypeReference.METHOD_TYPE_PARAMETER, TypeReference.METHOD_TYPE_PARAMETER_BOUND)

    override fun visitAnnotationDefault(): AnnotationVisitor =
        BinaryJavaAnnotationVisitor(context, signatureParser) {
            (member as? BinaryJavaMethod)?.annotationParameterDefaultValue = it
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

    override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String, visible: Boolean): AnnotationVisitor? {
        val typeReference = TypeReference(typeRef)

        fun getTargetType(baseType: JavaType) =
            if (typePath != null) {
                computeTargetType(baseType, typePath) to true
            } else {
                baseType to (typeReference.sort in freshlySupportedPositions)
            }

        val (annotationOwner, isFreshlySupportedAnnotation) = when (typeReference.sort) {
            TypeReference.METHOD_RETURN -> getTargetType((member as? BinaryJavaMethod)?.returnType ?: return null)
            TypeReference.METHOD_TYPE_PARAMETER -> member.typeParameters[typeReference.typeParameterIndex] to true
            TypeReference.METHOD_FORMAL_PARAMETER -> getTargetType(member.valueParameters[typeReference.formalParameterIndex].type)
            TypeReference.METHOD_TYPE_PARAMETER_BOUND -> getTargetType(
                BinaryJavaAnnotation.computeTypeParameterBound(member.typeParameters, typeReference)
            )
            else -> return null
        }

        if (annotationOwner !is MutableJavaAnnotationOwner) return null

        return BinaryJavaAnnotation.addAnnotation(annotationOwner, descriptor, context, signatureParser, isFreshlySupportedAnnotation)
    }
}

class BinaryJavaAnnotation private constructor(
    desc: String,
    private val context: ClassifierResolutionContext,
    override val arguments: Collection<JavaAnnotationArgument>,
    override val isFreshlySupportedTypeUseAnnotation: Boolean
) : JavaAnnotation {
    companion object {
        fun createAnnotationAndVisitor(
            desc: String,
            context: ClassifierResolutionContext,
            signatureParser: BinaryClassSignatureParser,
            isFreshlySupportedTypeUseAnnotation: Boolean = false
        ): Pair<JavaAnnotation, AnnotationVisitor> {
            val arguments = mutableListOf<JavaAnnotationArgument>()
            val annotation = BinaryJavaAnnotation(desc, context, arguments, isFreshlySupportedTypeUseAnnotation)

            return annotation to BinaryJavaAnnotationVisitor(context, signatureParser, arguments)
        }

        fun addAnnotation(
            annotationOwner: MutableJavaAnnotationOwner,
            desc: String,
            context: ClassifierResolutionContext,
            signatureParser: BinaryClassSignatureParser,
            isFreshlySupportedAnnotation: Boolean = false
        ): AnnotationVisitor {
            val (javaAnnotation, annotationVisitor) =
                createAnnotationAndVisitor(desc, context, signatureParser, isFreshlySupportedAnnotation)
            annotationOwner.annotations.add(javaAnnotation)
            return annotationVisitor
        }

        private fun translatePath(path: TypePath) = buildList {
            for (i in 0 until path.length) {
                when (val step = path.getStep(i)) {
                    // TODO: process inner types and apply an annotation to the corresponding type component
                    TypePath.INNER_TYPE -> continue
                    TypePath.ARRAY_ELEMENT, TypePath.WILDCARD_BOUND -> add(step to 0)
                    TypePath.TYPE_ARGUMENT -> add(step to path.getStepArgument(i))
                }
            }
        }

        internal fun computeTargetType(baseType: JavaType, typePath: TypePath) =
            translatePath(typePath).fold<Pair<Int, Int>, JavaType?>(baseType) { targetType, (typePathKind, typeArgumentIndex) ->
                when (typePathKind) {
                    TypePath.TYPE_ARGUMENT -> {
                        require(targetType is JavaClassifierType)
                        targetType.typeArguments.getOrNull(typeArgumentIndex) // temporary fix for KT-46131
                    }
                    TypePath.WILDCARD_BOUND -> {
                        require(targetType is JavaWildcardType)
                        // below, returned `null` means annotated implicit lower and upper wildcard's bound,
                        // it isn't supported yet to load type use annotations (null is further ignored)
                        // TODO: consider taking into account such annotations through returning wildcard itself as a target type (KT-40498)
                        targetType.bound
                    }
                    TypePath.ARRAY_ELEMENT -> {
                        require(targetType is JavaArrayType)
                        targetType.componentType
                    }
                    else -> targetType
                }
            }

        internal fun computeTypeParameterBound(typeParameters: List<JavaTypeParameter>, typeReference: TypeReference): JavaClassifierType {
            val typeParameter = typeParameters[typeReference.typeParameterIndex]

            require(typeParameter is BinaryJavaTypeParameter) { "Type parameter must be a binary type parameter" }

            val boundIndex = if (typeParameter.hasImplicitObjectClassBound) {
                typeReference.typeParameterBoundIndex - 1
            } else {
                typeReference.typeParameterBoundIndex
            }

            return typeParameter.upperBounds.elementAt(boundIndex)
        }
    }

    private val classifierResolutionResult by lazy(LazyThreadSafetyMode.NONE) {
        context.resolveByInternalName(Type.getType(desc).internalName)
    }

    override val classId: ClassId
        get() = (classifierResolutionResult.classifier as? JavaClass)?.classId
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
        /**
         * There are cases when enum is an inner class of some class which is not related to current loading Java class.
         *   And in this cases `mapInternalNameToClassId` leaves `$` in name as is, which may lead to unresolved errors later
         *   in compiler
         *
         *     @Api(status = Api.Status.Ok) // classId will be /Api$Status.Ok
         *     public class NestedEnumInAnnotation {}
         *
         *     public @interface Api {
         *         Status status();
         *
         *         enum Status {
         *             Ok, Error;
         *         }
         *     }
         *
         * It's impossible to use `resolveByInternalName` (which always provides correct classId), because it may lead to
         *   StackOverflowError for cases when enum and annotation are declared in same outer class, which will lead to
         *   infinite loading of this class
         *
         *     public class NestedEnumArgument {
         *         public enum E {
         *             FIRST
         *         }
         *
         *         public @interface Anno {
         *             E value();
         *         }
         *
         *         @Anno(E.FIRST)
         *         void foo() {}
         *     }
         *
         * So to avoid such recursion and in the same time fix original case we use simple heuristic about names with $
         *   for enums in annotation arguments which contain `$` in internal name
         */
        val internalName = Type.getType(desc).internalName
        var enumClassId = context.mapInternalNameToClassId(internalName)
        if (enumClassId.asString().contains("$")) {
            enumClassId = context.convertNestedClassInternalNameWithSimpleHeuristic(internalName) ?: enumClassId
        }
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

sealed class PlainJavaAnnotationArgument(name: String?) : JavaAnnotationArgument {
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
