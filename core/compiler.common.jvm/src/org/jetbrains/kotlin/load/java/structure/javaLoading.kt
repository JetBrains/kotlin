/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure


fun JavaMember.isObjectMethodInInterface(): Boolean {
    return containingClass.isInterface && this is JavaMethod && isObjectMethod(this)
}

private fun isObjectMethod(method: JavaMethod): Boolean {
    return when (method.name.asString()) {
        "toString", "hashCode" -> {
            method.valueParameters.isEmpty()
        }
        "equals" -> {
            isMethodWithOneObjectParameter(method)
        }
        else -> false
    }
}

private fun isMethodWithOneObjectParameter(method: JavaMethod): Boolean {
    val parameters = method.valueParameters
    val type = parameters.singleOrNull()?.type as? JavaClassifierType ?: return false
    val classifier = type.classifier
    if (classifier is JavaClass) {
        val classFqName = classifier.fqName
        return classFqName != null && classFqName.asString() == "java.lang.Object"
    }
    return false
}
