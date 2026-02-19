// TARGET_BACKEND: JVM
// LAMBDAS: INDY
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
@file:OptIn(ExperimentalVersionOverloading::class)

class C {
    fun inTrailing(
        x: String,
        @IntroducedAt("1") y: Int = 1,
        @IntroducedAt("2") z: Boolean = true,
        block: (String) -> String = { x.uppercase() }
    ) = "$x/$y/$z/${block("")}"

    fun inArgument(
        x: String,
        @IntroducedAt("1") y: Int = 1,
        @IntroducedAt("2") z: (Int) -> Int = { y + 1 },
        block: (String) -> String
    ) = "$x/$y/${z(0)}/${block("")}"
}

fun test1() : String {
    val c = C()
    val f: (String) -> String = String::uppercase
    val m1 = C::class.java.getMethod("inTrailing", String::class.java, kotlin.jvm.functions.Function1::class.java)
    val m2 = C::class.java.getMethod("inTrailing", String::class.java, Int::class.java, kotlin.jvm.functions.Function1::class.java)

    val v1 = m1.invoke(c, "hello", f) as String
    val v2 = m2.invoke(c, "hello", 1, f) as String
    val v3 = c.inTrailing("hello", 1, true, f)

    return if ((v1 == v3) && (v2 == v3)) "O" else "Err1: $v1 $v2 $v3 "
}

fun test2() : String {
    val c = C()
    val f: (String) -> String = String::uppercase
    val z: (Int) -> Int = { 2 }
    val m1 = C::class.java.getMethod("inArgument", String::class.java, kotlin.jvm.functions.Function1::class.java)
    val m2 = C::class.java.getMethod("inArgument", String::class.java, Int::class.java, kotlin.jvm.functions.Function1::class.java)

    val v1 = m1.invoke(c, "hello" , f) as String
    val v2 = m2.invoke(c, "hello", 1, f) as String
    val v3 = c.inArgument("hello", 1, z, f)

    return if ((v1 == v3) && (v2 == v3)) "K" else " Err2: $v1 $v2 $v3"
}

fun box() = test1() + test2()
