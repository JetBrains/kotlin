package my.sample

class Inner {
    fun a() {
        my.sample.<caret>Inner.say()
    }

    companion object Inner {
        fun say() {}
    }
}
