// FIR_IDENTICAL
package usage

@Target(AnnotationTarget.TYPE)
annotation class Anno(val s: String)

fun implicitType1() = TopLevelObject.expectedType()

object TopLevelObject {
    fun expectedType2(): @Anno(privateConstVal) Int = 4
    private const val privateConstVal = "privateConstVal"
    fun expectedType(): @Anno(privateConstVal) Int = 4
}

fun implicitType2() = TopLevelObject.expectedType2()