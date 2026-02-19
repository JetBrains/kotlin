// BODY_RESOLVE
package lowlevel

@Target(
    AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
)
annotation class Anno(val position: String)

const val prop = "str"

interface A

fun f<caret>unc() {
    fun explicitType(): @Anno("return type $prop") List<@Anno("nested return type $prop") List<@Anno("nested nested return type $prop") Int>> = 0
    fun implicitType() = explicitType()
}
