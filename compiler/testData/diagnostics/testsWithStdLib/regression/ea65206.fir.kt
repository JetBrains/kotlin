class A {
    val x = arrayListOf<<!UPPER_BOUND_VIOLATED!>(A<<!SYNTAX!><!>>) -> Unit<!>>()

    // Here we got an exception during type comparison
    fun foo(){
        x.add {}
    }

}
