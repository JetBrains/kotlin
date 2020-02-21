/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter.markers

import com.intellij.codeInsight.daemon.GutterIconDescriptor
import com.intellij.icons.AllIcons
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlighterBundle

internal object KotlinLineMarkerOptions {
    val overriddenOption = GutterIconDescriptor.Option(
        "kotlin.overridden",
        KotlinHighlighterBundle.message("name.overridden.declaration"), AllIcons.Gutter.OverridenMethod
    )

    val implementedOption = GutterIconDescriptor.Option(
        "kotlin.implemented",
        KotlinHighlighterBundle.message("name.implemented.declaration"), AllIcons.Gutter.ImplementedMethod
    )

    val overridingOption = GutterIconDescriptor.Option(
        "kotlin.overriding",
        KotlinHighlighterBundle.message("name.overriding.declaration"), AllIcons.Gutter.OverridingMethod
    )

    val implementingOption =
        GutterIconDescriptor.Option(
            "kotlin.implementing",
            KotlinHighlighterBundle.message("name.implementing.declaration"),
            AllIcons.Gutter.ImplementingMethod
        )

    val actualOption = GutterIconDescriptor.Option(
        "kotlin.actual",
        KotlinHighlighterBundle.message("name.multiplatform.actual.declaration"), KotlinIcons.ACTUAL
    )

    val expectOption = GutterIconDescriptor.Option(
        "kotlin.expect",
        KotlinHighlighterBundle.message("name.multiplatform.expect.declaration"), KotlinIcons.EXPECT
    )

    val dslOption =
        GutterIconDescriptor.Option("kotlin.dsl", KotlinHighlighterBundle.message("name.dsl.markers"), KotlinIcons.DSL_MARKER_ANNOTATION)

    val options = arrayOf(
        overriddenOption, implementedOption,
        overridingOption, implementingOption,
        actualOption, expectOption,
        dslOption
    )
}