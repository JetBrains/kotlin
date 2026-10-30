@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

const val con<caret>stant: @Anno("type $prop") Int = 1
