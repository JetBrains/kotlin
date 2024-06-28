// FIR_IDENTICAL
@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

fun foo(param: @Anno("parameter type $prop") List<@Anno("nested parameter type $prop") Collection<@Anno("nested nested parameter type $prop") String>>) = param