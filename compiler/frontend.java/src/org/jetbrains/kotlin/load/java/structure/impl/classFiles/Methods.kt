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

import com.intellij.util.cls.ClsFormatException
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import java.text.CharacterIterator
import java.text.StringCharacterIterator

abstract class BinaryJavaMethodBase(
        override val access: Int,
        override val containingClass: JavaClass,
        val valueParameters: List<JavaValueParameter>,
        val typeParameters: List<JavaTypeParameter>,
        override val name: Name
) : JavaMember, MapBasedJavaAnnotationOwner, BinaryJavaModifierListOwner {
    override val annotationsByFqName by buildLazyValueForMap()

    override val annotations: Collection<JavaAnnotation> = ContainerUtil.newSmartList()

    companion object {
        private class MethodInfo(
                val returnType: JavaType,
                val typeParameters: List<JavaTypeParameter>,
                val valueParameterTypes: List<JavaType>
        )

        fun create(
                name: String,
                access: Int,
                desc: String,
                signature: String?,
                containingClass: JavaClass,
                parentContext: ClassifierResolutionContext,
                signatureParser: BinaryClassSignatureParser
        ): Pair<JavaMember, MethodVisitor> {
            val isConstructor = "<init>" == name
            val isVarargs = access.isSet(Opcodes.ACC_VARARGS)

            val isInnerClassConstructor = isConstructor && containingClass.outerClass != null && !containingClass.isStatic
            val isEnumConstructor = containingClass.isEnum && isConstructor
            val info: MethodInfo =
                    if (signature != null) {
                        val contextForMethod = parentContext.copyForMember()
                        parseMethodSignature(signature, signatureParser, contextForMethod).also {
                            contextForMethod.addTypeParameters(it.typeParameters)
                        }
                    } else
                        parseMethodDescription(desc, parentContext, signatureParser).let {
                            when {
                                isEnumConstructor ->
                                    // skip ordinal/name parameters for enum constructors
                                    MethodInfo(it.returnType, it.typeParameters, it.valueParameterTypes.drop(2))
                                isInnerClassConstructor ->
                                    // omit synthetic inner class constructor parameter
                                    MethodInfo(it.returnType, it.typeParameters, it.valueParameterTypes.drop(1))
                                else -> it
                            }
                        }

            val parameterTypes = info.valueParameterTypes
            val parameterList = ContainerUtil.newSmartList<JavaValueParameter>()
            val paramCount = parameterTypes.size
            for (i in 0..paramCount - 1) {
                val type = parameterTypes[i]
                val isEllipsisParam = isVarargs && i == paramCount - 1

                parameterList.add(BinaryJavaValueParameter(null, type, isEllipsisParam))
            }

            val member: BinaryJavaMethodBase =
                    if (isConstructor)
                        BinaryJavaConstructor(access, containingClass, parameterList, info.typeParameters)
                    else
                        BinaryJavaMethod(
                                access, containingClass, parameterList, info.typeParameters, Name.identifier(name), info.returnType
                        )

            val paramIgnoreCount = when {
                isEnumConstructor -> 2
                isInnerClassConstructor -> 1
                else -> 0
            }

            return member to AnnotationsCollectorMethodVisitor(member, parentContext, signatureParser, paramIgnoreCount)
        }

        private fun parseMethodDescription(
                desc: String,
                context: ClassifierResolutionContext,
                signatureParser: BinaryClassSignatureParser
        ): MethodInfo {
            val returnType = signatureParser.mapAsmType(Type.getReturnType(desc), context)
            val parameterTypes = Type.getArgumentTypes(desc).map { signatureParser.mapAsmType(it, context) }

            return MethodInfo(returnType, emptyList(), parameterTypes)
        }

        private fun parseMethodSignature(
                signature: String,
                signatureParser: BinaryClassSignatureParser,
                context: ClassifierResolutionContext
        ): MethodInfo {
            val iterator = StringCharacterIterator(signature)
            val typeParameters = signatureParser.parseTypeParametersDeclaration(iterator, context)

            if (iterator.current() != '(') throw ClsFormatException()
            iterator.next()
            val paramTypes: List<JavaType>
            if (iterator.current() == ')') {
                paramTypes = emptyList()
            }
            else {
                paramTypes = ContainerUtil.newSmartList()
                while (iterator.current() != ')' && iterator.current() != CharacterIterator.DONE) {
                    paramTypes.add(signatureParser.parseTypeString(iterator, context))
                }
                if (iterator.current() != ')') throw ClsFormatException()
            }
            iterator.next()

            val returnType = signatureParser.parseTypeString(iterator, context)

            return MethodInfo(returnType, typeParameters, paramTypes)
        }
    }
}

class BinaryJavaMethod(
        flags: Int,
        containingClass: JavaClass,
        valueParameters: List<JavaValueParameter>,
        typeParameters: List<JavaTypeParameter>,
        name: Name,
        override val returnType: JavaType
) : BinaryJavaMethodBase(
        flags, containingClass, valueParameters, typeParameters, name
), JavaMethod {
    override var hasAnnotationParameterDefaultValue: Boolean = false
}

class BinaryJavaConstructor(
        flags: Int,
        containingClass: JavaClass,
        valueParameters: List<JavaValueParameter>,
        typeParameters: List<JavaTypeParameter>
) : BinaryJavaMethodBase(
        flags, containingClass, valueParameters, typeParameters,
        SpecialNames.NO_NAME_PROVIDED
), JavaConstructor
