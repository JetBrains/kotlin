@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

context(List<@Anno("context receiver type $prop") Int>)
class Foo construct<caret>or()
