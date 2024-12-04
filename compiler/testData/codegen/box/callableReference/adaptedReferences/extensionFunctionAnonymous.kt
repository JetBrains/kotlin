val a : String.() -> String = fun String.(): String { return this }

fun foo(x: String.() -> String): String { return x("OK") }

fun box(): String {
    return foo(::a.get())
}