/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.Type.*

object MapPsiToAsmDesc {
    fun typeDesc(type: PsiType): String = when (type) {
        PsiType.VOID -> primitive(VOID_TYPE)

        PsiType.BOOLEAN -> primitive(BOOLEAN_TYPE)

        PsiType.CHAR -> primitive(CHAR_TYPE)
        PsiType.INT -> primitive(INT_TYPE)
        PsiType.BYTE -> primitive(BYTE_TYPE)
        PsiType.SHORT -> primitive(SHORT_TYPE)
        PsiType.LONG -> primitive(LONG_TYPE)

        PsiType.FLOAT -> primitive(FLOAT_TYPE)
        PsiType.DOUBLE -> primitive(DOUBLE_TYPE)

        is PsiArrayType -> "[" + typeDesc(type.componentType)

        is PsiClassType -> {
            val resolved = type.resolve()
            when (resolved) {
                is PsiTypeParameter -> resolved.superTypes.firstOrNull()?.let { typeDesc(it) } ?: "Ljava/lang/Object;"
                is PsiClass -> classDesc(resolved)
                null -> unknownSignature()
                else -> error("Resolved to unexpected $resolved of class ${resolved::class.java}")
            }

        }
        else -> error("Unexpected type $type of class ${type::class.java}")
    }

    private fun classDesc(psiClass: PsiClass) = buildString {
        append("L")
        val classes = generateSequence(psiClass) { it.containingClass }.toList().reversed()
        append(classes.first().qualifiedName!!.replace(".", "/"))
        classes.drop(1).forEach {
            append("$")
            append(it.name!!)
        }
        append(";")
    }

    fun methodDesc(psiMethod: PsiMethod): String = buildString {
        append("(")
        psiMethod.parameterList.parameters.forEach {
            append(typeDesc(it.type))
        }
        append(")")
        psiMethod.returnType?.let {
            append(typeDesc(it))
        }
                ?: return unknownSignature() // TODO: support constructors, there seems to be additional logic in java that doesn't work correctly for compiled kotlin
    }

    private fun unknownSignature() = ""
    private fun error(message: String): String {
        LOG.error(message)
        return unknownSignature()
    }

    private fun primitive(asmType: Type) = asmType.descriptor

    private val LOG = Logger.getInstance(this::class.java)
}