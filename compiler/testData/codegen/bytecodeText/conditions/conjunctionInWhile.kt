val a = false
val b = false
val c = false

fun main() {
    while (a && b && c) {
        "loop"
    }
}

// 0 ICONST_0
// 0 ICONST_1
// 3 IFEQ
// 0 IFNE
// 3 IF
// 1 GOTO