open class C(p: Int) {
    open fun f(){}
}

fun foo(p1: String?, p2: String?) {
    if (p1 == null) return
    println(p1)

    val c = object : C(p1.length) {
        override fun f() {
            super.f()
            if (p2 == null) return
            print(p2.<caret>length)
            bar()
        }

        fun bar() {
            print(1)
        }
    }
}