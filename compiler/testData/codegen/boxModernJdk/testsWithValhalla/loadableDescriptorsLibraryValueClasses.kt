// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_TEXT

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// The standard library is compiled without Valhalla support, so its value classes are identity classes at runtime. They are listed
// anyway, since this compilation decides which classes are value classes: such a library is expected to be recompiled with Valhalla
// support, and listing an identity class has no effect.
class LibraryHolder(val u: UInt?, val r: Result<Int>?, val d: Duration?)

fun box(): String {
    val holder = LibraryHolder(1u, Result.success(2), 3.seconds)
    if (holder.u != 1u) return "LibraryHolder.u: ${holder.u}"
    if (holder.r?.getOrNull() != 2) return "LibraryHolder.r: ${holder.r}"
    if (holder.d != 3.seconds) return "LibraryHolder.d: ${holder.d}"
    return "OK"
}

// 1 ATTRIBUTE LoadableDescriptors
// 1 ATTRIBUTE LoadableDescriptors : Lkotlin/UInt;, Lkotlin/Result;, Lkotlin/time/Duration;\n
