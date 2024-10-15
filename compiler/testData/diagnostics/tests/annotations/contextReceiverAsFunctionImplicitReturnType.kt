// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextReceivers

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(List<@Anno("context receiver type $prop") Int>)
fun foo() = this@List

const val prop = "str"
