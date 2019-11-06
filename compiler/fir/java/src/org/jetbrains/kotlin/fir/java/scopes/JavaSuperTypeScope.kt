/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirSuperTypeScope

class JavaSuperTypeScope(
    klass: FirRegularClass,
    session: FirSession,
    scopes: List<FirScope>
) : FirSuperTypeScope(
    session,
    JavaOverrideChecker(session, if (klass is FirJavaClass) klass.javaTypeParameterStack else JavaTypeParameterStack.EMPTY),
    scopes
)