/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.scopes.FirImportingScope
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirSelfImportingScope(val fqName: FqName, val session: FirSession) : FirImportingScope {
    override fun processClassifiersByName(name: Name, processor: (ClassId) -> Boolean): Boolean {


        val unambiguousFqName = ClassId(fqName, name)

        val firProvider = session.service<FirProvider>()

        if (firProvider.getFirClassifierByFqName(unambiguousFqName) != null) {
            return processor(unambiguousFqName)
        } else {
            return true
        }
    }
}