annotation class Ann(val x: Long, val s: String)

fun test() {
    <!WRONG_ANNOTATION_TARGET!>@Ann(s = "hello", x = 1)<!> String::class
}
