// MOVE: down

fun foo(x: Boolean) {
    when (x) {
        false -> {

        }
        true -> {

        }
        else -> {
            <caret>// test
        }
    }
}
