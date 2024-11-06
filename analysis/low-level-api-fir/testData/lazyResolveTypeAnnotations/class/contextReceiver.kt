import prop

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

context(List<@Anno("context receiver type $prop") Int>)
class Fo<caret>o
