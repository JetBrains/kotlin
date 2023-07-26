// RENDER_DIAGNOSTICS_FULL_TEXT

data class Example(val a: String, val b: Int) {
    fun testRedeclaration(e: Example){
        val (<!REDECLARATION!>b<!>, <!REDECLARATION!>b<!>) = e
    }

}
