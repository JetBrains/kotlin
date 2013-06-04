// "Change 'bar' type to '(() -> Long) -> Unit'" "true"
fun foo() {
    val bar: () -> Double = {
        (f: () -> Long): String ->
        var x = 5<caret>
    }
}
