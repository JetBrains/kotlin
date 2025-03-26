// RESOLVE_FILE
@file:Anno(fun(): Int {
    val s: Int = "str"
    return s
})

package one

@Target(AnnotationTarget.FILE)
annotation class Anno(val s: String)

fun topLevelFunction(l: Long) = 0