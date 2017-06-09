// "Replace elvis with equality check" "true"

fun foo() {
    var a: Boolean? = null
    var b: Boolean? = null
    if (a ?: false || !(b<caret> ?: true)) {

    }
}