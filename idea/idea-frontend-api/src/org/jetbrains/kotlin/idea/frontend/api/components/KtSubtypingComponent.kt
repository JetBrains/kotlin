/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.types.KtType

abstract class KtSubtypingComponent : KtAnalysisSessionComponent() {
    abstract fun isEqualTo(first: KtType, second: KtType): Boolean
    abstract fun isSubTypeOf(subType: KtType, superType: KtType): Boolean
}

interface KtSubtypingComponentMixIn : KtAnalysisSessionMixIn {
    infix fun KtType.isEqualTo(other: KtType): Boolean =
        analysisSession.subtypingComponent.isEqualTo(this, other)

    infix fun KtType.isSubTypeOf(superType: KtType): Boolean =
        analysisSession.subtypingComponent.isSubTypeOf(this, superType)

    infix fun KtType.isNotSubTypeOf(superType: KtType): Boolean =
        !analysisSession.subtypingComponent.isSubTypeOf(this, superType)
}