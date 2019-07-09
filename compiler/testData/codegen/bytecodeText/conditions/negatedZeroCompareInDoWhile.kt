// Generates ICONST_1
val a = 1

fun main() {
    // Generates IFNE
    do {
        "loop"
    } while (!(a == 0))

    // Generates IFNE
    do {
        "loop"
    } while (a != 0)

    // Generates IFLE
    do {
        "loop"
    } while (!(a > 0))

    // Generates IFLT
    do {
        "loop"
    } while (!(a >= 0))

    // Generates IFGE
    do {
        "loop"
    } while (!(a < 0))

    // Generates IFGT
    do {
        "loop"
    } while (!(a <= 0))
}

//0 ICONST_0
//1 ICONST_1
//2 IFNE
//1 IFLE
//1 IFLT
//1 IFGE
//1 IFGT
//6 IF
//0 GOTO