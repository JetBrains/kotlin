class C {
    fun foo() = object {
        fun test1() = this@C.toString() + this.toString()
        fun test2() = { this@C.toString() + this.toString() }
    }
}