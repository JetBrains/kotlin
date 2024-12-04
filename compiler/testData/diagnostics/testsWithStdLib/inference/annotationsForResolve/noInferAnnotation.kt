// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: Java.java

class Java {
    static <K> void g0(K k) { }
    static void g1(In<String> k) { }
}

// FILE: Kotlin.kt

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> test1(t1: T, t2: @kotlin.internal.NoInfer T): T = t1

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> @kotlin.internal.NoInfer T.test2(t1: T): T = t1

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> test3(t1: @kotlin.internal.NoInfer T): T = t1

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> test4(t1: T, t2: List<@kotlin.internal.NoInfer T>): T = t1

class In<in T>
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <E> id(e: E): In<@kotlin.internal.NoInfer E> = TODO()

fun test5(x: In<String>) {}
fun In<String>.test6() {}

open class A
class B : A()
class Out<out T>
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <E> f2(e: E): Out<@kotlin.internal.NoInfer E> = TODO()
fun <T> test7(t: T, x: Out<T>) {}

fun usage(y: Int) {
    test1(1, <!TYPE_MISMATCH("Int; String")!>"312"<!>)
    1.test2("")
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test3<!>("")
    test4(1, <!TYPE_MISMATCH("List<Int>; List<String>")!>listOf("a")<!>)
    val x: In<String> = <!TYPE_MISMATCH!>id(y)<!>
    test5(<!TYPE_MISMATCH!>id(y)<!>)
    id(y).<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>test6<!>()
    test7(B(), f2(A()))
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <E> f(): @kotlin.internal.NoInfer E = TODO()

fun flexibleTypes(y: Int) {
    Java.g0(f<Int>())
    Java.g1(<!TYPE_MISMATCH!>id(y)<!>)
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> List<T>.contains1(e: @kotlin.internal.NoInfer T): Boolean = true

fun test(i: Int?, a: Any, l: List<Int>) {
    l.contains1(<!TYPE_MISMATCH!>a<!>)
    l.contains1(<!TYPE_MISMATCH!>""<!>)
    l.contains1(<!TYPE_MISMATCH!>i<!>)
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> assertEquals1(e1: T, e2: @kotlin.internal.NoInfer T): Boolean = true

fun test(s: String) {
    assertEquals1(s, <!CONSTANT_EXPECTED_TYPE_MISMATCH!>11<!>)
}

fun interface Predicate<in T> {
    fun accept(i: T): Boolean
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> testSamParameterType(a: Predicate<@kotlin.internal.NoInfer T>, b: Predicate<T>): T = TODO()

fun test() {
    testSamParameterType(<!TYPE_MISMATCH!>{ x: String -> false }<!>, { x: CharSequence -> true })
}
