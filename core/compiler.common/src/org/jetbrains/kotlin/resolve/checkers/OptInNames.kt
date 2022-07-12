/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object OptInNames {
    val OLD_EXPERIMENTAL_FQ_NAME = FqName("kotlin.Experimental")
    val OLD_USE_EXPERIMENTAL_FQ_NAME = FqName("kotlin.UseExperimental")
    val REQUIRES_OPT_IN_FQ_NAME = FqName("kotlin.RequiresOptIn")
    val REQUIRES_OPT_IN_CLASS_ID = ClassId.topLevel(REQUIRES_OPT_IN_FQ_NAME)
    val OPT_IN_FQ_NAME = FqName("kotlin.OptIn")
    val OPT_IN_CLASS_ID = ClassId.topLevel(OPT_IN_FQ_NAME)
    val SUBCLASS_OPT_IN_REQUIRED_FQ_NAME = FqName("kotlin.SubclassOptInRequired")
    val SUBCLASS_OPT_IN_REQUIRED_CLASS_ID = ClassId.topLevel(SUBCLASS_OPT_IN_REQUIRED_FQ_NAME)

    val WAS_EXPERIMENTAL_FQ_NAME = FqName("kotlin.WasExperimental")
    val WAS_EXPERIMENTAL_CLASS_ID = ClassId.topLevel(WAS_EXPERIMENTAL_FQ_NAME)
    val USE_EXPERIMENTAL_ANNOTATION_CLASS = Name.identifier("markerClass")
    val WAS_EXPERIMENTAL_ANNOTATION_CLASS = Name.identifier("markerClass")

    val REQUIRES_OPT_IN_FQ_NAMES = setOf(OLD_EXPERIMENTAL_FQ_NAME, REQUIRES_OPT_IN_FQ_NAME)
    val OPT_IN_FQ_NAMES = setOf(OLD_USE_EXPERIMENTAL_FQ_NAME, OPT_IN_FQ_NAME)

    @Deprecated(
        message = "EXPERIMENTAL_FQ_NAMES is deprecated, please use REQUIRES_OPT_IN_FQ_NAMES instead",
        ReplaceWith("REQUIRES_OPT_IN_FQ_NAMES", imports = ["org.jetbrains.kotlin.resolve.checkers.OptInNames.REQUIRES_OPT_IN_FQ_NAMES"])
    )
    @Suppress("unused")
    val EXPERIMENTAL_FQ_NAMES = REQUIRES_OPT_IN_FQ_NAMES

    @Deprecated(
        message = "USE_EXPERIMENTAL_FQ_NAMES is deprecated, please use OPT_IN_FQ_NAMES instead",
        ReplaceWith("OPT_IN_FQ_NAMES", imports = ["org.jetbrains.kotlin.resolve.checkers.OptInNames.OPT_IN_FQ_NAMES"])
    )
    @Suppress("unused")
    val USE_EXPERIMENTAL_FQ_NAMES = OPT_IN_FQ_NAMES

    fun buildDefaultDiagnosticMessage(prefix: String, markerName: String): String {
        return "$prefix with '@$markerName' or '@OptIn($markerName::class)'"
    }

    fun buildMessagePrefix(verb: String): String =
        "This declaration needs opt-in. Its usage $verb be marked"

    fun buildOverrideMessage(supertypeName: String, markerMessage: String?, verb: String, markerName: String): String {
        val basePrefix = "Base declaration of supertype '$supertypeName' needs opt-in. "
        val markerMessageOrStub = markerMessage
            ?.takeIf { it.isNotBlank() }?.let { if (it.endsWith(".")) "$it " else "$it. " } ?: ""
        val baseSuffix = buildDefaultDiagnosticMessage("The declaration override $verb be annotated", markerName)
        return basePrefix + markerMessageOrStub + baseSuffix
    }
}