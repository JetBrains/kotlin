// FIR_IDENTICAL
// ISSUE: KT-83824
// JSPECIFY_STATE: strict
// LANGUAGE: +CheckPackageInfoNullnessAnnotations

// FILE: api/package-info.java

@NullMarked
package api;

import org.jspecify.annotations.NullMarked;

// FILE: api/JavaInvOfStringInheritor.java

package api;

public interface JavaInvOfStringInheritor extends JavaInv<String> {}

// FILE: api/JavaInv.java

package api;

public interface JavaInv<NC> {}

// FILE: test.kt

package dsl

import kotlin.reflect.*
import api.*

class Inv1<RP>
class Inv2<ED>

class MixOfJavaInvStringAndInheritor : JavaInv<String>, JavaInvOfStringInheritor

/**
 * RT <: Any
 * RC <: JavaInv<RT>
 * MixOfJavaInvStringAndInheritor! <: RC
 * PDT <: Any
 * PDC <: JavaInv<PDT>
 * Inv1<CapturedType(out RC)> <: Inv1<PDC>
 *
 * PDC = CapturedType(out RC)
 */

val <RT : Any, RC : JavaInv<RT>> RC.registering: Inv1<out RC>
    get() = TODO()

operator fun <PDT : Any, PDC : JavaInv<PDT>> Inv1<PDC>.provideDelegate(
    receiver: Any?,
    property: KProperty<*>
): Inv2<Collection<PDT>> = TODO()

operator fun <GV> Inv2<out GV>.getValue(receiver: Any?, property: KProperty<*>): GV = TODO()

fun foo(t: MixOfJavaInvStringAndInheritor) {
    val valley123: Collection<String> by t.registering
}
