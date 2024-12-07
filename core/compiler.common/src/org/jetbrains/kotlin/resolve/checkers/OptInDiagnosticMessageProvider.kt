/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers


abstract class OptInDiagnosticMessageProvider {

    abstract fun buildDefaultDiagnosticMessage(markerName: String, verb: String = ""): String
    open fun buildCustomDiagnosticMessage(message: String): String = message
}

object OptInUsagesDiagnosticMessageProvider : OptInDiagnosticMessageProvider() {
    override fun buildDefaultDiagnosticMessage(markerName: String, verb: String): String =
        OptInNames.buildDefaultDiagnosticMessage("This declaration needs opt-in. Its usage $verb be marked", markerName)

}

object OptInUsagesInFutureDiagnosticMessageProvider : OptInDiagnosticMessageProvider() {
    override fun buildDefaultDiagnosticMessage(markerName: String, verb: String): String =
        OptInNames.buildDefaultDiagnosticMessage(
            "This declaration is experimental due to signature types and its usage $verb be marked (will become an error in future releases)",
            markerName
        )
}

class OptInInheritanceDiagnosticMessageProvider(private val isSubclassOptInApplicable: Boolean) : OptInDiagnosticMessageProvider() {
    companion object {
        private const val DEFAULT_PREFIX = "This class or interface requires opt-in to be implemented"
    }

    override fun buildDefaultDiagnosticMessage(markerName: String, verb: String): String =
        OptInNames.buildDefaultDiagnosticMessage("$DEFAULT_PREFIX. Its usage $verb be marked", markerName, isSubclassOptInApplicable)

    override fun buildCustomDiagnosticMessage(message: String): String = "$DEFAULT_PREFIX: $message"

}

object ExperimentalUnsignedLiteralsDiagnosticMessageProvider : OptInDiagnosticMessageProvider() {
    override fun buildDefaultDiagnosticMessage(markerName: String, verb: String): String =
        OptInNames.buildDefaultDiagnosticMessage("Unsigned literals are experimental and their usages $verb be marked", markerName)

}