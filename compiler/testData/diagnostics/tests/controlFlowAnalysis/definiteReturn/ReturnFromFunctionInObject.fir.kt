interface X {
    fun f(): Boolean
}

val m = object : X {
    override fun f(): Boolean {
    }

    fun foo() {
        fun local(): Int {
        }
    }
}
