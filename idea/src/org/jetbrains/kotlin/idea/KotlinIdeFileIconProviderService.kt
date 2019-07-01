/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.impl.ElementPresentationUtil
import com.intellij.util.PlatformIcons
import javax.swing.Icon

class KotlinIdeFileIconProviderService : KotlinIconProviderService() {
    override fun getFileIcon(): Icon = KOTLIN_FILE

    override fun getLightVariableIcon(element: PsiModifierListOwner, flags: Int): Icon {
        val baseIcon = ElementPresentationUtil.createLayeredIcon(PlatformIcons.VARIABLE_ICON, element, false)
        return ElementPresentationUtil.addVisibilityIcon(element, flags, baseIcon)
    }

    companion object {
        private val KOTLIN_FILE = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_file.svg")
    }
}