// IGNORE_BACKEND: JVM_IR
fun box(): String {
    var boo = "OK"
    var foo = object {
        val bar = object {
            val baz = boo
        }
    }

    return foo.bar.baz
}