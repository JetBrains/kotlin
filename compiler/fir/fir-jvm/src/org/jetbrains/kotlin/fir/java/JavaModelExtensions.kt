/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaEnumValueAnnotationArgument
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.load.java.structure.JavaType

/**
 * `java-direct`-specific protocols on FIR-side Java-model consumers.
 *
 * **Step C** (per `compiler/java-direct/implDocs/INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md`):
 * these subinterfaces relocate three protocols off the public
 * `core/compiler.common.jvm` Java-model interfaces. PSI/binary
 * impls do not need them — they pre-filter / pre-evaluate / pre-disambiguate at
 * structure-build time. java-direct opts in by implementing these subinterfaces; FIR-side
 * callers downcast (`as?`) before reading. The public Java-model interface surface stays
 * free of `java-direct` debt; the §1 invariant in the inventory is preserved.
 *
 * Defined in `fir-jvm` (rather than in `compiler/java-direct/`) because fir-jvm is the
 * consumer; java-direct already depends on fir-jvm transitively via `:compiler:frontend.java`,
 * but fir-jvm does not depend on java-direct, so locating the protocol here avoids any
 * dependency cycle.
 */

/**
 * java-direct-private TYPE_USE annotation pre-filtering protocol.
 *
 * PSI/binary impls pre-filter at structure-build time (javac-wrapper does this at the
 * Java structure level), so they don't implement this. java-direct cannot pre-filter
 * without resolving annotation FQNs first, so it implements this and lets the caller
 * supply a `isTypeUseAnnotation(fqName)` callback.
 */
interface JavaTypeWithExternalAnnotationFiltering : JavaType {
    /** Whether [filterTypeUseAnnotations] does anything beyond returning [annotations]. */
    val needsTypeUseAnnotationFiltering: Boolean

    /**
     * Filters [annotations] to only TYPE_USE annotations.
     *
     * @param isTypeUseAnnotation callback receiving an annotation class FQN, returning `true`
     *   if the class is a TYPE_USE annotation.
     */
    fun filterTypeUseAnnotations(isTypeUseAnnotation: (String) -> Boolean): Collection<JavaAnnotation>
}

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
