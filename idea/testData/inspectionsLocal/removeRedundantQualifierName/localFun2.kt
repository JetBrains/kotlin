package my.simple.name

class Inner {
    fun a() {
        fun say(i: Inner) {}
        Inner<caret>.say()
    }

    companion object {
        fun say() {}
    }
}
