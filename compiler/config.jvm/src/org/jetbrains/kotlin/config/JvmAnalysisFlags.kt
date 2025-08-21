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
    val javaTypeEnhancementState by Delegates.JavaTypeEnhancementStateNullByDefault

    @JvmStatic
    val jvmDefaultMode by Delegates.JvmDefaultModeNullByDefault

    @JvmStatic
    val inheritMultifileParts by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val sanitizeParentheses by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val suppressMissingBuiltinsError by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val enableJvmPreview by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val outputBuiltinsMetadata by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val expectBuiltinsAsPartOfStdlib by AnalysisFlag.Delegates.Boolean

    private object Delegates {
        object JavaTypeEnhancementStateNullByDefault {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>): AnalysisFlag.Delegate<JavaTypeEnhancementState?> =
                AnalysisFlag.Delegate(property.name, null)
        }

        object JvmDefaultModeNullByDefault {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>): AnalysisFlag.Delegate<JvmDefaultMode?> =
                AnalysisFlag.Delegate(property.name, null)
        }
    }
}
