fun foo(p: (String) -> Unit){}
fun foo(p: (Int) -> Unit){}

fun bar() {
    foo(<caret>)
}

// ABSENT: "{...}"
// EXIST: "{ String -> ... }"
// EXIST: "{ Int -> ... }"
