/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.scopes.FirImportingScope
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FirCompositeImportingScope(vararg val scopes: FirImportingScope) : FirImportingScope {
    override fun processClassifiersByName(name: Name, processor: (ClassId) -> Boolean): Boolean {
        for (scope in scopes) {
            if (!scope.processClassifiersByName(name, processor)) {
                return false
            }
        }
        return true
    }
}