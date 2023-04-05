@Target(AnnotationTarget.CLASS) @Anno("string")
annotation class ResolveMe(val value: Int)

annotation class Anno(val s: String)