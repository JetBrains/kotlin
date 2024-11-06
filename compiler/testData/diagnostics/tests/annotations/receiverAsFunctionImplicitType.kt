// FIR_IDENTICAL
@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

fun List<@Anno("context receiver type $prop") Int>.foo() = this

const val prop = "str"
