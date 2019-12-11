class A {
    val x = arrayListOf<(A<<!SYNTAX!><!>>) -> Unit>()

    // Here we got an exception during type comparison
    fun foo(){
        x.add {}
    }

}
