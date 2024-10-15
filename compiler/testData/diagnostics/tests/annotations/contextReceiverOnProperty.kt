// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextReceivers
// ISSUE: KT-72863

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(List<@Anno("context receiver type $prop") Int>)
val property: Int get() = 0

const val prop = "str"
