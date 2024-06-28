/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.fir.backend.FirMangleComputer
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.isMaybeMainFunction
import org.jetbrains.kotlin.fir.java.findJvmNameValue
import org.jetbrains.kotlin.fir.java.findJvmStaticAnnotation
import org.jetbrains.kotlin.fir.resolve.providers.firProvider

/**
 * JVM backend-specific mangle computer that generates a mangled name for a Kotlin declaration represented by [FirDeclaration].
 */
class FirJvmMangleComputer(
    builder: StringBuilder,
    mode: MangleMode,
) : FirMangleComputer(builder, mode) {

    override fun FirFunction.platformSpecificSuffix(): String? =
        // this implementation doesn't cover all cases supported in JvmDescriptorMangler, because it uses a weaker check (see comment
        // at isMaybeMainFunction).So it may add prefix to more functions. But because a "correct" implementation may require some
        // complex machinery in Fir (we need to traverse the tree completely in a separate phase), while the problems and benefits are
        // unclear, we decided that this implementation is good enough for now.
        if ((this as? FirSimpleFunction)?.isMaybeMainFunction(
                getPlatformName = { findJvmNameValue() },
                isPlatformStatic = { findJvmStaticAnnotation() != null },
            ) == true
        )
            this.moduleData.session.firProvider.getFirCallableContainerFile(symbol)?.name
        else null

    override fun addReturnType(): Boolean = true

    override fun copy(newMode: MangleMode): FirJvmMangleComputer =
        FirJvmMangleComputer(builder, newMode)
}
