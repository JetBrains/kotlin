// WITH_RUNTIME
object X {
    var string = "foo"
}

fun main() {
    X.string <caret>= "bar"
}
