// KT-90199
@Target(AnnotationTarget.TYPE)
annotation class Anno(val s: String)

@Target(AnnotationTarget.PROPERTY)
annotation class PropertyAnno(val s: String)

const val CONST = "const"

data class MyPair(val i: Int, val b: Int)

class A {
    val (first, @PropertyAnno(CONST) la<caret>st: @Anno(CONST) Int) = MyPair(1, 2)
}
