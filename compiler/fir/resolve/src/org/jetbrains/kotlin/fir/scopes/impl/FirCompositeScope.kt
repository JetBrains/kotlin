/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FirCompositeScope(val scopes: MutableList<FirScope>) : FirScope {
    override fun processClassifiersByName(name: Name, processor: (ClassId) -> Boolean): Boolean {
        for (scope in scopes) {
            if (!scope.processClassifiersByName(name, processor)) {
                return false
            }
        }
        return true
    }
}