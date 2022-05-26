/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin

internal fun javaOrigin(isFromSource: Boolean): FirDeclarationOrigin.Java {
    return if (isFromSource) FirDeclarationOrigin.Java.Source else FirDeclarationOrigin.Java.Library
}
