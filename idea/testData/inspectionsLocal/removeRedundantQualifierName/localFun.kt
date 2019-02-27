package my.simple.name

class Inner {
    fun a() {
        fun Inner() {}
        Inner<caret>.say()
    }

    companion object {
        fun say() {}
    }
}
