// "Change 'f' type to '(Int, Int) -> (String) -> Int'" "true"
fun foo() {
    val f: (Int, Int) -> (String) -> Int = {
        (a: Int, b: Int): (String) -> Int ->
        val x = {(s: String) -> 42}
        if (true) x
        else if (true) x else {
            var y = 42
            if (true) x<caret> else x
        }
    }
}