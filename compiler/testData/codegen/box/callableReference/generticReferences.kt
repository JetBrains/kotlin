// IGNORE_BACKEND_K1: ANY
// DUMP_IR
// DUMP_IR_AFTER_INLINE


class A<T> {
    fun bar(x: T): T = x
}

fun <T> foo(x: T): T = x
fun <T> A<T>.foo(x: T): T = bar(x)

class A2<T: CharSequence> {
    fun bar2(x: T): T = x
}

fun <T : CharSequence> foo2(x: T): T = x
fun <T : CharSequence> A2<T>.foo2(x: T): T = bar2(x)


class A3<T: Comparable<T>> {
    fun bar3(x: T): T = x
}

fun <T : Comparable<T>> foo3(x: T): T = x
fun <T : Comparable<T>> A3<T>.foo3(x: T): T = bar3(x)


fun box(): String {
    val r1: (Int) -> Int = ::foo
    val r2 = A<Int>::foo
    val r3 = A<*>::foo
    val r4 = A<Int>::bar
    val r5 = A<*>::bar


    val s1: (String) -> String = ::foo2
    val s2 = A2<String>::foo2
    val s3 = A2<*>::foo2
    val s4 = A2<String>::bar2
    val s5 = A2<*>::bar2

    val t1: (String) -> String = ::foo3
    val t2 = A3<String>::foo3
    val t3 = A3<*>::foo3
    val t4 = A3<String>::bar3
    val t5 = A3<*>::bar3

    return "OK"
}