/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import org.jetbrains.kotlin.idea.KotlinIcons
import javax.swing.Icon

class IdeaKonanIconProvider : KonanIconProvider {
    override fun getExecutableIcon(): Icon = KotlinIcons.NATIVE
}