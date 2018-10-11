// PROBLEM: none
fun test() {
    foo()<caret>;
    /**
     * kdoc
     */
    { i: Int ->  }.doIt()
}
fun foo() {}
fun ((Int) -> Unit).doIt() {}
