val a = false
val b = false
val c = false

fun main() {
    do {
        "loop"
    } while (a && b && c)
}

// 0 ICONST_0
// 0 ICONST_1
// 2 IFEQ
// 1 IFNE
// 3 IF
// 0 GOTO