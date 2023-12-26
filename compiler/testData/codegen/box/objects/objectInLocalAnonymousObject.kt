// JVM_ABI_K1_K2_DIFF: KT-63655
fun box(): String {
    var boo = "OK"
    var foo = object {
        val bar = object {
            val baz = boo
        }
    }

    return foo.bar.baz
}