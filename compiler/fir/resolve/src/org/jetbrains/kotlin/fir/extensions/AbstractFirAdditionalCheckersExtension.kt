/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import kotlin.reflect.KClass

abstract class AbstractFirAdditionalCheckersExtension(session: FirSession) : FirExtension(session) {
    fun interface Factory : FirExtension.Factory<AbstractFirAdditionalCheckersExtension>

    final override val extensionType: KClass<out FirExtension>
        get() = AbstractFirAdditionalCheckersExtension::class
}