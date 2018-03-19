/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.UnambiguousFqName
import org.jetbrains.kotlin.name.Name

interface FirImportingScope {
    fun processClassifiersByName(name: Name, processor: (UnambiguousFqName) -> Boolean): Boolean
}