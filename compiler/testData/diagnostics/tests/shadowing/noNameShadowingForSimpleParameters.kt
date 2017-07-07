// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_ANONYMOUS_PARAMETER

open class Base {
    open fun foo(name: String) {}
}

fun test1(name: String) {
    class Local : Base() {
        override fun foo(name: String) {
        }
    }
}

fun test2(param: String) {
    fun local(param: String) {}
}

fun test3(param: String) {
    fun local() {
        fff { param -> }
    }
}

fun fff(x: (y: String) -> Unit) {}