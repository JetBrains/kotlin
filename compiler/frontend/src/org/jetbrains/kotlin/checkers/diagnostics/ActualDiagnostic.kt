/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers.diagnostics

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters1

class ActualDiagnostic constructor(val diagnostic: Diagnostic, override val platform: String?, withNewInference: Boolean) :
    AbstractTestDiagnostic {
    override var inferenceCompatibility = if (withNewInference)
        TextDiagnostic.InferenceCompatibility.NEW
    else
        TextDiagnostic.InferenceCompatibility.OLD

    override val name: String
        get() = diagnostic.factory.name!!

    val file: PsiFile
        get() = diagnostic.psiFile!!

    override fun compareTo(other: AbstractTestDiagnostic): Int {
        return if (this.diagnostic is DiagnosticWithParameters1<*, *> && other is ActualDiagnostic && other.diagnostic is DiagnosticWithParameters1<*, *>) {
            (name + this.diagnostic.a).compareTo(other.name + other.diagnostic.a)
        } else if (this.diagnostic is DiagnosticWithParameters1<*, *>) {
            (name + this.diagnostic.a).compareTo(other.name)
        } else if (other is ActualDiagnostic && other.diagnostic is DiagnosticWithParameters1<*, *>) {
            name.compareTo(other.name + other.diagnostic.a)
        } else {
            name.compareTo(other.name)
        }
    }

    override fun enhanceInferenceCompatibility(inferenceCompatibility: TextDiagnostic.InferenceCompatibility) {
        this.inferenceCompatibility = inferenceCompatibility
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ActualDiagnostic) return false

        // '==' on diagnostics is intentional here
        return other.diagnostic === diagnostic &&
                (if (other.platform == null) platform == null else other.platform == platform) &&
                other.inferenceCompatibility == inferenceCompatibility
    }

    override fun hashCode(): Int {
        var result = System.identityHashCode(diagnostic)
        result = 31 * result + (platform?.hashCode() ?: 0)
        result = 31 * result + inferenceCompatibility.hashCode()
        return result
    }

    override fun toString(): String {
        val inferenceAbbreviation = inferenceCompatibility.abbreviation
        return (if (inferenceAbbreviation != null) inferenceAbbreviation + ";" else "") +
                (if (platform != null) "$platform:" else "") +
                diagnostic.toString()
    }
}