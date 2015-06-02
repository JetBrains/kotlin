class A {
    val x = arrayListOf<(A<<!SYNTAX!><!>>) -> Unit>()

    // Here we got an exception during type comparison
    fun foo(){
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>.add {}
    }

}
