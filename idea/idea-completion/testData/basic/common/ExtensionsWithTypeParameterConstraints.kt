// FIR_COMPARISON
fun <T : Int> T.ext1() {}
fun <T : String> T.ext2() {}

fun test() {
    with(42) {
        ext<caret>
    }
}

// EXIST: ext1
// ABSENT: ext2