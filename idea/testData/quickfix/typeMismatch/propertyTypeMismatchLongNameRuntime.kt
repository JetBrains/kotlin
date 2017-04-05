// "Change type of 'f' to '(Delegates) -> Unit'" "true"

fun foo() {
    var f: Int = { x: kotlin.properties.Delegates ->  }<caret>
}