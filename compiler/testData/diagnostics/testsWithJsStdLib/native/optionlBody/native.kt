native
val baz: Int
native
val boo: Int = noImpl

native
val Int.baz: Int

native
fun foo()
native
fun bar() {}

native
fun String.foo(): Int
native
fun String.bar(): Int = noImpl

native
trait T {
    val baz: Int

    fun foo()
    fun bar() {}

    default object {
        val baz: Int
        val boo: Int = noImpl

        fun foo()
        fun bar(): String = noImpl
    }
}

native
class C {
    val baz: Int
    val boo: Int = noImpl

    fun foo()
    fun bar() {}

    default object {
        val baz: Int
        val boo: Int = noImpl

        fun foo()
        fun bar(): String = noImpl
    }
}

native
object O {
    val baz: Int
    val boo: Int = noImpl

    fun foo(s: String): String
    fun bar(s: String): String = noImpl
}

fun test() {
    [native]
    class Local {
        val baz: Int
        val boo: Int = noImpl

        fun foo()
        fun bar() {}
    }
}

