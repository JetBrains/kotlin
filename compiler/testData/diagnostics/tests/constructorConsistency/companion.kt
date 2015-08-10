class My {

    val x = <!DEBUG_INFO_LEAKING_THIS!>foo<!>()

    val w = bar()

    fun foo() = 0

    companion object {
        
        val y = <!UNRESOLVED_REFERENCE!>foo<!>()

        val u = <!DEBUG_INFO_LEAKING_THIS!>bar<!>()

        val z: String? = bar()

        fun bar() = "1"
    }
}