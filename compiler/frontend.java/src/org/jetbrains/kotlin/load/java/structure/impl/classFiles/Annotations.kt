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
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Type
import java.lang.reflect.Array

internal class AnnotationsCollectorMethodVisitor(
        private val member: BinaryJavaMethodBase,
        private val context: ClassifierResolutionContext,
        private val signatureParser: BinaryClassSignatureParser,
        private val parametersToSkipNumber: Int
) : MethodVisitor(ASM_API_VERSION_FOR_CLASS_READING) {
    override fun visitAnnotationDefault(): AnnotationVisitor? {
        member.safeAs<BinaryJavaMethod>()?.hasAnnotationParameterDefaultValue = true
        // We don't store default value in Java model
        return null
    }

    override fun visitAnnotation(desc: String, visible: Boolean) =
            BinaryJavaAnnotation.addAnnotation(
                    member.annotations as MutableCollection<JavaAnnotation>,
                    desc, context, signatureParser
            )

    override fun visitParameterAnnotation(parameter: Int, desc: String, visible: Boolean): AnnotationVisitor? {
        val index = parameter - parametersToSkipNumber
        if (index < 0) return null

        val annotations =
                member.valueParameters[index].annotations as MutableCollection<JavaAnnotation>?
                ?: return null

        return BinaryJavaAnnotation.addAnnotation(annotations, desc, context, signatureParser)
    }
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
                annotations: MutableCollection<JavaAnnotation>,
                desc: String,
                context: ClassifierResolutionContext,
                signatureParser: BinaryClassSignatureParser
        ): AnnotationVisitor {
            val (javaAnnotation, annotationVisitor) = createAnnotationAndVisitor(desc, context, signatureParser)
            annotations.add(javaAnnotation)

            return annotationVisitor
        }
    }

    private val classifierResolutionResult by lazy(LazyThreadSafetyMode.NONE) {
        context.resolveByInternalName(Type.getType(desc).internalName)
    }

    override val classId: ClassId?
        get() = classifierResolutionResult.classifier.safeAs<JavaClass>()?.classId()
                ?: ClassId.topLevel(FqName(classifierResolutionResult.qualifiedName))

    override fun resolve() = classifierResolutionResult.classifier as? JavaClass
}

class BinaryJavaAnnotationVisitor(
        private val context: ClassifierResolutionContext,
        private val signatureParser: BinaryClassSignatureParser,
        private val arguments: MutableCollection<JavaAnnotationArgument>
) : AnnotationVisitor(ASM_API_VERSION_FOR_CLASS_READING) {
    private fun addArgument(argument: JavaAnnotationArgument?) {
        arguments.addIfNotNull(argument)
    }

    override fun visitAnnotation(name: String?, desc: String): AnnotationVisitor {
        val (annotation, visitor) =
                BinaryJavaAnnotation.createAnnotationAndVisitor(desc, context, signatureParser)

        arguments.add(PlainJavaAnnotationAsAnnotationArgument(name, annotation))

        return visitor
    }

    override fun visitEnum(name: String?, desc: String, value: String) {
         addArgument(PlainJavaEnumValueAnnotationArgument(name, desc, value, context))
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
        private val desc: String,
        entryName: String,
        private val context: ClassifierResolutionContext
) : PlainJavaAnnotationArgument(name), JavaEnumValueAnnotationArgument {
    override val entryName = Name.identifier(entryName)

    override fun resolve(): JavaField? {
        val javaClass = context.resolveByInternalName(Type.getType(desc).internalName).classifier as? JavaClass ?: return null
        return javaClass.fields.singleOrNull { it.name == entryName }
    }
}

private fun JavaClass.classId(): ClassId? {
    val fqName = fqName ?: return null
    if (outerClass == null) return ClassId.topLevel(fqName)

    val outerClassId = outerClass!!.classId() ?: return null

    return ClassId(outerClassId.packageFqName, outerClassId.relativeClassName.child(name), false)
}
