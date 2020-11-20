// FIR_IDENTICAL
// WITH_REFLECT
import kotlin.reflect.KCallable

fun defaultsOnly(x: String = "") = 1

fun regularAndDefaults(x1: String, x2: String = "") = 1

fun varargs(vararg xs: String) = 1

class C(val x: String = "")

fun useKCallableStar(fn: KCallable<*>) {}


fun testDefaultsOnlyStar() { useKCallableStar(::defaultsOnly) }

fun testRegularAndDefaultsStar() { useKCallableStar(::regularAndDefaults) }

fun testVarargsStar() { useKCallableStar(::varargs) }

fun testCtorStar() { useKCallableStar(::C) }
