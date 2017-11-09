open class C(p: Int) {
    open fun f(){}
}

fun foo(p: String?) {
    val o = object : Runnable {
        override fun run() {
            if (p == null) return
            print(p.length)
        }
    }

    val c = object : C(p!!.size) {
        override fun f() {
            super.f()
            if (p == null) return
            print(p.length)
        }
    }

    <caret>p?.length
}