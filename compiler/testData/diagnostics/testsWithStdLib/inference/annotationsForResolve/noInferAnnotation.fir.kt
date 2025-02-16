// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: Java.java

class Java {
    static <K> void g0(K k) { }
    static void g1(In<String> k) { }
}

// FILE: Kotlin.kt

@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
fun <T> test1(t1: T, t2: @kotlin.internal.NoInfer T): T = t1

@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
fun <T> @kotlin.internal.NoInfer T.test2(t1: T): T = t1

@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
fun <T> test3(t1: @kotlin.internal.NoInfer T): T = t1

@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
fun <T> test4(t1: T, t2: List<@kotlin.internal.NoInfer T>): T = t1

class In<in T>
@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
fun <E> id(e: E): In<@kotlin.internal.NoInfer E> = TODO()

fun test5(x: In<String>) {}
fun In<String>.test6() {}

open class A
class B : A()
class Out<out T>
@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
fun <E> f2(e: E): Out<@kotlin.internal.NoInfer E> = TODO()
fun <T> test7(t: T, x: Out<T>) {}

fun usage(y: Int) {
    <!TYPE_MISMATCH!>test1<!>(1, "312")
    1.<!TYPE_MISMATCH!>test2<!>("")
    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test3<!>("")
    <!TYPE_MISMATCH("String; Int")!>test4<!>(1, listOf("a"))
    val x: In<String> = <!INITIALIZER_TYPE_MISMATCH!><!TYPE_MISMATCH!>id<!>(y)<!>
    <!TYPE_MISMATCH!>test5<!>(id(y))
    id(y).<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>test6<!>()
    test7(B(), f2(A()))
}

@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
fun <E> f(): @kotlin.internal.NoInfer E = TODO()

fun flexibleTypes(y: Int) {
    Java.g0(f<Int>())
    Java.<!TYPE_MISMATCH!>g1<!>(id(y))
}

@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
fun <T> List<T>.contains1(e: @kotlin.internal.NoInfer T): Boolean = true

fun test(i: Int?, a: Any, l: List<Int>) {
    l.<!TYPE_MISMATCH!>contains1<!>(a)
    l.<!TYPE_MISMATCH!>contains1<!>("")
    l.<!TYPE_MISMATCH!>contains1<!>(i)
}

@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
fun <T> assertEquals1(e1: T, e2: @kotlin.internal.NoInfer T): Boolean = true

fun test(s: String) {
    <!TYPE_MISMATCH!>assertEquals1<!>(s, 11)
}

fun interface Predicate<in T> {
    fun accept(i: T): Boolean
}

@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
fun <T> testSamParameterType(a: Predicate<@kotlin.internal.NoInfer T>, b: Predicate<T>): T = TODO()

fun test() {
    <!TYPE_MISMATCH!>testSamParameterType<!>({ x: String -> false }, { x: CharSequence -> true })
}
