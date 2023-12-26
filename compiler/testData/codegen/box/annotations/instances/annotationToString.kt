// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: WASM
// DONT_TARGET_EXACT_BACKEND: JS

// This test fails on Native with test grouping and package renaming enabled,
// because the latter doesn't yet handle annotation toString implementations properly.
// Disable test grouping as a workaround:
// NATIVE_STANDALONE

// WITH_STDLIB
// !LANGUAGE: +InstantiationOfAnnotationClasses

// JVM_ABI_K1_K2_DIFF: KT-62465

package test

import kotlin.reflect.KClass

enum class E { E0 }
annotation class Empty

annotation class A(
    val b: Byte,
    val s: Short,
    val i: Int,
    val f: Float,
    val d: Double,
    val l: Long,
    val c: Char,
    val bool: Boolean
)

annotation class Anno(
    val s: String,
    val i: Int,
    val f: Double,
    val u: UInt,
    val e: E,
    val a: A,
    val k: KClass<*>,
    val arr: Array<String>,
    val intArr: IntArray,
    val arrOfE: Array<E>,
    val arrOfA: Array<Empty>,
)

fun box(): String {
    val anno = Anno(
        "OK", 42, 2.718281828, 43u, E.E0,
        A(1, 1, 1, 1.0.toFloat(), 1.0, 1, 'c', true),
        A::class, emptyArray(), intArrayOf(1, 2), arrayOf(E.E0), arrayOf(Empty())
    )
    val s = anno.toString()
    val targetJVM = "@test.Anno(s=OK, i=42, f=2.718281828, u=43, e=E0, a=@test.A(b=1, s=1, i=1, f=1.0, d=1.0, l=1, c=c, bool=true), " +
            "k=interface test.A, arr=[], intArr=[1, 2], arrOfE=[E0], arrOfA=[@test.Empty()])"
    val targetJS = "@test.Anno(s=OK, i=42, f=2.718281828, u=43, e=E0, a=@test.A(b=1, s=1, i=1, f=1, d=1, l=1, c=c, bool=true), k=class A, arr=[...], intArr=[...], arrOfE=[...], arrOfA=[...])"
    val targetNative = targetJVM
        .replace(" (Kotlin reflection is not available)", "")
        .replace("interface", "class")
    return if (s == targetJS || s == targetJVM || s == targetNative) "OK" else "FAILED, got string $s"
}
