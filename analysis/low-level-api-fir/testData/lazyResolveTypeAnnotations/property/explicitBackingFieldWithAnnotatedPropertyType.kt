@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

open class A {
    val pr<caret>operty: @Anno("type $prop") Any
        field = 1
}
