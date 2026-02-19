@Target(AnnotationTarget.TYPE)
annotation class Anno(val number: Int)

const val value = 0

fun typeWithAnnotation(): @Anno(value) String = ""

fun res<caret>olveMe() = typeWithAnnotation()