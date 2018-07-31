// PROBLEM: none
fun test() {
    foo()<caret>;
    @Ann("")
    { i: Int ->  }.doIt()
}
fun foo() {}
fun ((Int) -> Unit).doIt() {}

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Ann(val bar: String)