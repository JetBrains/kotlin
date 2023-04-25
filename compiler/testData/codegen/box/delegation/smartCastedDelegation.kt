interface DosFileAttributeView {
    fun bar(): String
}

fun <V> foo(view: V): DosFileAttributeView {
    view as DosFileAttributeView
    return object : DosFileAttributeView by view {}
}

fun box() = foo(object : DosFileAttributeView {
    override fun bar() = "OK"
}).bar()