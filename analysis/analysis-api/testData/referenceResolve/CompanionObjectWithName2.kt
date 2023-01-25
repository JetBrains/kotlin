package my.sample

class Inner {
    fun a() {
        my.<caret>sample.Inner.say()
    }

    companion object Inner {
        fun say() {}
    }
}
