// TRUE

// Problem with lazy initialization of nullable properties
trait Some

val test: Some = object: Some {
    fun test() {
        <caret>
    }
}