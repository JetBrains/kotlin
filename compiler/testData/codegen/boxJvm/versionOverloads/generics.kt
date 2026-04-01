// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// WITH_SIGNATURES
// WITH_STDLIB
@file:OptIn(ExperimentalVersionOverloading::class)

class C {
    fun <A> foo(
        a: Int = 1,
        @IntroducedAt("1") b: A? = null,
        @IntroducedAt("2") c: Boolean = true,
    ) = "$a/$b/$c"

    fun <A> bar(
        a: A,
        @IntroducedAt("1") b: A = a,
        @IntroducedAt("2") c: Boolean = true,
    ) = "$a/$b/$c"
}

fun test1() : String {
    val c = C()
    val m1 = C::class.java.getMethod("foo", Int::class.java)
    val m2 = C::class.java.getMethod("foo", Int::class.java, Object::class.java)

    val v1 = m1.invoke(c, 10) as String
    val v2 = m2.invoke(c, 10, null) as String
    val v3 = c.foo(10, null, true)

    return if ((v1 == v2) && (v1 == v3)) "O" else "Err1: $v1 $v2 $v3 "
}

fun test2() : String {
    val c = C()
    val m1 = C::class.java.getMethod("bar", Object::class.java)
    val m2 = C::class.java.getMethod("bar", Object::class.java, Object::class.java)

    val v1 = m1.invoke(c, "hello") as String
    val v2 = m2.invoke(c, "hello", "hello") as String
    val v3 = c.bar("hello", "hello", true)

    return if ((v1 == v2) && (v1 == v3)) "K" else " Err2: $v1 $v2 $v3"
}

fun box() = test1() + test2()
