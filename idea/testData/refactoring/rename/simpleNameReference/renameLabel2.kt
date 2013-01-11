class AA {
    class B {
        fun Int.bar() {
            val a = this@AA
            val b = this@B

            val c = this
            val c1 = this@b<caret>ar
        }
    }
}