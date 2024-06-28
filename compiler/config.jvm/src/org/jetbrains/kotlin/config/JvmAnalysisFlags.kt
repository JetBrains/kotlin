/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.load.java.JavaTypeEnhancementState
import kotlin.reflect.KProperty

object JvmAnalysisFlags {
    @JvmStatic
    val strictMetadataVersionSemantics by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val javaTypeEnhancementState by Delegates.JavaTypeEnhancementStateWarnByDefault

    @JvmStatic
    val jvmDefaultMode by Delegates.JvmDefaultModeDisabledByDefault

    @JvmStatic
    val inheritMultifileParts by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val sanitizeParentheses by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val suppressMissingBuiltinsError by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val enableJvmPreview by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val useIR by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val generatePropertyAnnotationsMethods by AnalysisFlag.Delegates.Boolean

    private object Delegates {
        object JavaTypeEnhancementStateWarnByDefault {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>): AnalysisFlag.Delegate<JavaTypeEnhancementState> =
                AnalysisFlag.Delegate(property.name, JavaTypeEnhancementState.DEFAULT)
        }

        object JvmDefaultModeDisabledByDefault {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>): AnalysisFlag.Delegate<JvmDefaultMode> =
                AnalysisFlag.Delegate(property.name, JvmDefaultMode.DISABLE)
        }
    }
}
