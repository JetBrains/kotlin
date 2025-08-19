/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.config.LanguageFeature

open class CommonKlibBasedCompilerArgumentsConfigurator : CommonCompilerArgumentsConfigurator() {
    override fun configureExtraLanguageFeatures(arguments: CommonCompilerArguments, map: HashMap<LanguageFeature, LanguageFeature.State>) {
        require(arguments is CommonKlibBasedCompilerArguments)
        if (arguments.irInlinerBeforeKlibSerialization) {
            map[LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.ENABLED
            map[LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization] = LanguageFeature.State.ENABLED
        }
    }
}
