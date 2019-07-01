/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter.markers

import com.intellij.codeInsight.daemon.GutterIconDescriptor
import com.intellij.icons.AllIcons
import org.jetbrains.kotlin.idea.KotlinIcons

internal object KotlinLineMarkerOptions {
    val overriddenOption = GutterIconDescriptor.Option("kotlin.overridden", "Overridden declaration", AllIcons.Gutter.OverridenMethod)

    val implementedOption = GutterIconDescriptor.Option("kotlin.implemented", "Implemented declaration", AllIcons.Gutter.ImplementedMethod)

    val overridingOption = GutterIconDescriptor.Option("kotlin.overriding", "Overriding declaration", AllIcons.Gutter.OverridingMethod)

    val implementingOption =
        GutterIconDescriptor.Option("kotlin.implementing", "Implementing declaration", AllIcons.Gutter.ImplementingMethod)

    val actualOption = GutterIconDescriptor.Option("kotlin.actual", "Multiplatform actual declaration", KotlinIcons.ACTUAL)

    val expectOption = GutterIconDescriptor.Option("kotlin.expect", "Multiplatform expect declaration", KotlinIcons.EXPECT)

    val dslOption = GutterIconDescriptor.Option("kotlin.dsl", "DSL markers", KotlinIcons.DSL_MARKER_ANNOTATION)

    val options = arrayOf(
        overriddenOption, implementedOption,
        overridingOption, implementingOption,
        actualOption, expectOption,
        dslOption
    )
}