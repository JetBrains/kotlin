// LANGUAGE: +ContextReceivers

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(List<@Anno("context receiver type $prop") Int>)
val foo get() = this@List

const val prop = "str"
