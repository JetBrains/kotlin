class My {

    val x = foo()

    val w = bar()

    fun foo() = 0

    companion object {
        
        val y = foo()

        val u = bar()

        val z: String? = bar()

        fun bar() = "1"
    }
}