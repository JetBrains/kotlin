/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.load.java.structure.JavaEnumValueAnnotationArgument
import org.jetbrains.kotlin.load.java.structure.JavaField

/**
 * `java-direct`-specific protocols on FIR-side Java-model consumers.
 *
 * Historically also held a `JavaTypeWithExternalAnnotationFiltering` interface used as a
 * model→FIR callback bridge for TYPE_USE annotation filtering. That interface was retired on
 * 2026-05-25 in favour of pre-filtering at `JavaTypeOverAst.annotations`-read time, driven by
 * `JavaResolutionContext.isTypeUseAnnotationClass` (which owns the `FirSession`-backed
 * `@Target` probe and its per-session cache). See `JTC_CLEANUP_2026_05_24.md` "Critical
 * analysis (2026-05-25)" for the rationale.
 */

/**
 * java-direct-private cross-language constant-evaluation protocol.
 *
 * PSI/binary fields already evaluate Java-side constant expressions at structure-build time
 * and cannot have a non-Java reference, so they don't implement this. java-direct implements
 * this for fields whose initializer is a non-literal reference that may need cross-language
 * resolution (Java field referencing a Kotlin `const val`).
 */
interface JavaFieldWithExternalInitializerResolution : JavaField {
    /** Whether [resolveInitializerValue] does anything beyond returning [initializerValue]. */
    val supportsExternalInitializerResolution: Boolean

    /**
     * Resolves the initializer value using a callback that can resolve external references.
     *
     * @param resolveReference callback resolving a qualified reference (e.g., `OtherClass.FIELD`)
     *   to its constant value; returns `null` if the reference cannot be resolved.
     * @return the evaluated constant value, or `null` if evaluation fails.
     */
    fun resolveInitializerValue(resolveReference: (classQualifier: String?, fieldName: String) -> Any?): Any?
}

/**
 * java-direct-private enum-vs-const-field disambiguation protocol.
 *
 * PSI/binary classifiers statically tell `KConstsKt.WARNING` (a const) from
 * `RetentionPolicy.RUNTIME` (an enum entry) at structure-build time, so they don't
 * implement this. java-direct cannot disambiguate at parse time and emits
 * `JavaEnumValueAnnotationArgument` for both cases, opting into the FIR-side const-field
 * fallback by implementing this subinterface and returning `true` from
 * [couldBeConstReference].
 */
interface JavaEnumValueAnnotationArgumentWithConstFallback : JavaEnumValueAnnotationArgument {
    val couldBeConstReference: Boolean
}
