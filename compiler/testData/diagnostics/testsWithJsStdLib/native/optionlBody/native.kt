// !DIAGNOSTICS: -UNREACHABLE_CODE
// unreachable code suppressed due to KT-9586

@native
val baz: Int
@native
val boo: Int = noImpl

@native
val Int.baz: Int

@native
fun foo()
@native
fun bar() {}

@native
fun String.foo(): Int
@native
fun String.bar(): Int = noImpl

@native
interface T {
    val baz: Int

    fun foo()
    fun bar() {}

    companion object {
        val baz: Int
        val boo: Int = noImpl

        fun foo()
        fun bar(): String = noImpl
    }
}

@native
class C {
    val baz: Int
    val boo: Int = noImpl

    fun foo()
    fun bar() {}

    companion object {
        val baz: Int
        val boo: Int = noImpl

        fun foo()
        fun bar(): String = noImpl
    }
}

@native
object O {
    val baz: Int
    val boo: Int = noImpl

    fun foo(s: String): String
    fun bar(s: String): String = noImpl
}

fun test() {
    @native
    class Local {
        val baz: Int
        val boo: Int = noImpl

        fun foo()
        fun bar() {}
    }
}

