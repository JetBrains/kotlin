// FALSE
trait Some

fun test() {
    val foo = object: Some {
        <caret>
    }
}