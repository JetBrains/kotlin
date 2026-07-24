/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.CommonClassNames
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

fun KtClassOrObject.defaultJavaAncestorQualifiedName(): String? {
    if (this !is KtClass) return CommonClassNames.JAVA_LANG_OBJECT

    return when {
        isAnnotation() -> CommonClassNames.JAVA_LANG_ANNOTATION_ANNOTATION
        isEnum() -> CommonClassNames.JAVA_LANG_ENUM
        isInterface() -> CommonClassNames.JAVA_LANG_OBJECT // see com.intellij.psi.impl.PsiClassImplUtil.getSuperClass
        else -> CommonClassNames.JAVA_LANG_OBJECT
    }
}
