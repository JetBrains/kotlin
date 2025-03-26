@file:Anno(fun(): Int {
    <expr>val s: Int = "str"</expr>
    return s
})

package one

@Target(AnnotationTarget.FILE)
annotation class Anno(val s: String)
