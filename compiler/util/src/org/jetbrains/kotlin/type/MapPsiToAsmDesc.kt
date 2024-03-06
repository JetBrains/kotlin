/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.type

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.Type.*

object MapPsiToAsmDesc {
    fun typeDesc(type: PsiType): String = when (type) {
        PsiTypes.voidType() -> primitive(VOID_TYPE)

        PsiTypes.booleanType() -> primitive(BOOLEAN_TYPE)

        PsiTypes.charType() -> primitive(CHAR_TYPE)
        PsiTypes.intType() -> primitive(INT_TYPE)
        PsiTypes.byteType() -> primitive(BYTE_TYPE)
        PsiTypes.shortType() -> primitive(SHORT_TYPE)
        PsiTypes.longType() -> primitive(LONG_TYPE)

        PsiTypes.floatType() -> primitive(FLOAT_TYPE)
        PsiTypes.doubleType() -> primitive(DOUBLE_TYPE)

        is PsiArrayType -> "[" + typeDesc(type.componentType)

        is PsiClassType -> {
            when (val resolved = type.resolve()) {
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
        append(psiMethod.returnType?.let { typeDesc(it) } ?: "V")
    }

    private fun unknownSignature() = ""
    private fun error(message: String): String {
        LOG.error(message)
        return unknownSignature()
    }

    private fun primitive(asmType: Type) = asmType.descriptor

    private val LOG = Logger.getInstance(this::class.java)
}