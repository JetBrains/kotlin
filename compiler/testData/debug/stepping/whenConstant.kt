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
// JVM_IR does not use a switch in this case and therefore steps to the evaluation
// of the condition for each of the cases.

// LINENUMBERS
// test.kt:4 box
// LINENUMBERS JVM_IR
// test.kt:5 box
// test.kt:7 box
// LINENUMBERS
// test.kt:10 box
// test.kt:12 box