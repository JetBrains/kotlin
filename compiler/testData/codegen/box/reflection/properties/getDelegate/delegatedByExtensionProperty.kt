// WITH_STDLIB
val x: String.() -> String = { this }
val String.y: String.() -> String
    get() = { this + this@y }

val String.c: String.() -> String by String::y
val String.d: (String) -> String by String::y

fun box(): String {
    val a: (String) -> String by ::x
    val b: String.() -> String by ::x
    return if (a("1")+"2".b()+"4".c("3")+"6".d("5")=="123456") "OK" else "fail"
}