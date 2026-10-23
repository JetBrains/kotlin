@Target(AnnotationTarget.TYPE)
annotation class Anno(val number: Int)

const val value = 0

fun typeWithAnnotation(): @Anno(value) String = ""

<expr>var resolveMe
    get() = typeWithAnnotation()
    set(value) {}</expr>
