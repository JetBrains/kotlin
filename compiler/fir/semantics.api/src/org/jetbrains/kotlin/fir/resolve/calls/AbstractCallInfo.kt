/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.name.Name

abstract class AbstractCallInfo {
    abstract val callSite: FirElement
    abstract val name: Name
    abstract val containingFile: FirFile
    abstract val isImplicitInvoke: Boolean
    abstract val explicitReceiver: FirExpression?
    abstract val argumentList: FirArgumentList
}
