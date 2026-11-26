/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.psiUtil.containingScript

/**
 * The qualified name of the light class for this class or object, or `null` if it has no [fqName][KtClassOrObject.getFqName].
 *
 * Classes declared in a script are nested in the script class on the JVM, so the name includes the script name,
 * unlike [fqName][KtClassOrObject.getFqName].
 */
@KtImplementationDetail
@OptIn(KtExperimentalApi::class)
fun KtClassOrObject.getLightClassQualifiedName(): String? {
    val qualifiedName = fqName?.asString() ?: return null
    val script = KtPsiUtil.getOutermostClassOrObject(this).containingScript ?: return qualifiedName
    val scriptFqName = script.fqName
    val packageFqName = scriptFqName.parent()
    val relativeName = if (packageFqName.isRoot) qualifiedName else qualifiedName.substring(packageFqName.asString().length + 1)
    return "${scriptFqName.asString()}.$relativeName"
}
