// "Replace elvis with equality check" "true"

fun foo() {
    var a: Boolean? = null
    if (!(a <caret>?: false)) {

    }
}