// ISSUE: KT-87549
// CHECK_BYTECODE_LISTING

open class A {
    open fun foo(a: String = "a", b: String = "b"): String = a + b
}

@OptIn(ExperimentalVersionOverloading::class)
class B : A() {
    override fun foo(a: String, @IntroducedAt("1") b: String): String = a + b
}

fun box(): String {
    val b = B()
    val foo1 = B::class.java.getMethod("foo", String::class.java, String::class.java)
    val foo2 = B::class.java.getMethod("foo", String::class.java)

    val r1 = foo1.invoke(b, "O", "K") as String
    val r2 = foo2.invoke(b, "O") as String

    return when {
        r1 == "OK" && r2 == "Ob" -> "OK"
        else -> "Fail. r1: $r1; r2:$r2"
    }
}
