// "Change type of 'f' to '(Delegates) -> Unit'" "true"
// WITH_RUNTIME

fun foo() {
    var f: Int = { x: kotlin.properties.Delegates ->  }<caret>
}