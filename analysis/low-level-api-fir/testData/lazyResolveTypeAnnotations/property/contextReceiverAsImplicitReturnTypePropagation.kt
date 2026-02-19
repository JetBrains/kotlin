@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

val List<Int>.b<caret>ar get() = foo

context(List<@Anno("context receiver type $prop") Int>)
val foo get() = this@List