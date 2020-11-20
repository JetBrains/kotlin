// WITH_RUNTIME
object X {
    var string = "foo"
}

var target = "baz"
fun main() {
    target <caret>= X.string
}
