/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.lifetime

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenFactory

public class KtReadActionConfinementLifetimeTokenProvider : KtLifetimeTokenProvider() {
    override fun getLifetimeTokenFactory(): KtLifetimeTokenFactory {
        return KtReadActionConfinementLifetimeTokenFactory
    }
}
