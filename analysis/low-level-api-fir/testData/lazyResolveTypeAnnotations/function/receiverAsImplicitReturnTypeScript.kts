@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

fun @Anno("receiver type $prop") List<@Anno("nested receiver type $prop") Collection<@Anno("nested nested receiver type $prop") String>>.f<caret>oo() = this