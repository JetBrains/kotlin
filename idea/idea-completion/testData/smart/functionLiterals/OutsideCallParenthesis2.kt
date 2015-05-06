fun foo(p: String.(Int) -> Unit){}

fun bar(p: String.(Int) -> Unit) {
    foo()<caret>
}

// EXIST: "{...}"
// EXIST: "{ Int -> ... }"
// ABSENT: p
