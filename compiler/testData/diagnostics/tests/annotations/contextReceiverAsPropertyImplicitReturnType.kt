// LANGUAGE: +ContextReceivers

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(List<@Anno("context receiver type $prop") Int>)
val foo get() = this@List

const val prop = "str"
