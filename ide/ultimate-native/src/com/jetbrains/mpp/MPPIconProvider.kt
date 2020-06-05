/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.jetbrains.konan.KonanIconProvider
import org.jetbrains.kotlin.idea.KotlinIcons
import javax.swing.Icon

class MPPIconProvider : KonanIconProvider {
    override fun getExecutableIcon(): Icon = KotlinIcons.NATIVE
}