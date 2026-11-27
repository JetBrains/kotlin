// LANGUAGE: +RichErrors
// ISSUE: KT-92031

error class Foo(val x: String, y: Int) {
    val z: Boolean = true
}

fun box(): String {
    val s = Foo("a", 1).toString()
    if (s != "Foo(x=a)") return s
    return "OK"
}
