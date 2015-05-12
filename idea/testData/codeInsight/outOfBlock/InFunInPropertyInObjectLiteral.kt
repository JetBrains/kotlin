// FALSE
interface Some

fun test() {
    val foo = object: Some {
        <caret>
    }
}