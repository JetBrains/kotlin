@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

fun List<Int>.b<caret>ar() = foo()

context(List<@Anno("context receiver type $prop") Int>)
fun foo() = this@List