/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.utils.Jsr305State
import kotlin.reflect.KProperty

object JvmAnalysisFlags {
    @JvmStatic
    val strictMetadataVersionSemantics by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val jsr305 by Delegates.Jsr305StateWarnByDefault

    @JvmStatic
    val jvmDefaultMode by Delegates.JvmDefaultModeDisabledByDefault

    @JvmStatic
    val inheritMultifileParts by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val sanitizeParentheses by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val suppressMissingBuiltinsError by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val irCheckLocalNames by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val disableUltraLightClasses by AnalysisFlag.Delegates.Boolean

    private object Delegates {
        object Jsr305StateWarnByDefault {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>): AnalysisFlag.Delegate<Jsr305State> =
                AnalysisFlag.Delegate(property.name, Jsr305State.DEFAULT)
        }

        object JvmDefaultModeDisabledByDefault {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>): AnalysisFlag.Delegate<JvmDefaultMode> =
                AnalysisFlag.Delegate(property.name, JvmDefaultMode.DISABLE)
        }
    }
}
