/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction

fun KtElement.isMainFunction(computedDescriptor: DeclarationDescriptor? = null): Boolean {
    if (this !is KtNamedFunction) return false
    val mainFunctionDetector = MainFunctionDetector(languageVersionSettings) { it.resolveToDescriptorIfAny() }

    if (computedDescriptor != null) return mainFunctionDetector.isMain(computedDescriptor)

    return mainFunctionDetector.isMain(this)
}
