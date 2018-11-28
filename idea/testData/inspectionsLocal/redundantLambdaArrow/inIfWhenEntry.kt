// PROBLEM: none
fun test(): (Int) -> Int {
    return when {
        true -> { <caret>_ -> 42 }
        else -> { _ -> 42 }
    }
}