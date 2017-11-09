fun manyParams(used: String, unused: String) = println(used)

fun callMany() {
    <caret>manyParams("op", "qr")
}