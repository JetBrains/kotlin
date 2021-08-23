// FILE: test.kt

fun box() {
    when (1) {
        2 ->
            "2"
        3 ->
            "3"
        else ->
            "1"
    }
}

// JVM_IR and JVM backends have different heuristics for when to use a switch.
// JVM_IR inlines constant value subject and folds unreachable branches.

// LINENUMBERS
// test.kt:4 box
// test.kt:10 box
// test.kt:12 box