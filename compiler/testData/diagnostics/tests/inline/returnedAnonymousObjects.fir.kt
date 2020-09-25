private inline fun foo(crossinline f: () -> Int) = object {
    fun bar(): Int = f()
}

fun test(b: Boolean) {
    var x = foo { 1 }
    if (b) {
        x = foo { 2 }
    }
    x.bar()
}