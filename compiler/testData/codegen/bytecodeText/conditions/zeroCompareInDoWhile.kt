// Generates ICONST_1
val a = 1

fun main() {
    // Generates IFEQ
    do {
        "loop"
    } while (a == 0)

    // Generates IFGT
    do {
        "loop"
    } while (a > 0)

    // Generates IFGE
    do {
        "loop"
    } while (a >= 0)

    // Generates IFLT
    do {
        "loop"
    } while (a < 0)

    // Generates IFLE
    do {
        "loop"
    } while (a <= 0)
}

//0 ICONST_0
//1 ICONST_1
//1 IFEQ
//1 IFLE
//1 IFLT
//1 IFGE
//1 IFGT
//5 IF
//0 GOTO