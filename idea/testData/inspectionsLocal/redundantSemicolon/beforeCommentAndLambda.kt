// PROBLEM: none
fun test() {
    val foo = ""<caret>;
    // comment
    { i: Int ->  }.doIt()
}
fun ((Int) -> Unit).doIt() {}
