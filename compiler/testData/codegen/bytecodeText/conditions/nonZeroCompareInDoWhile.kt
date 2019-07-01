// Generates ICONST_1
val a = 1

fun main() {
    // Generates IF_ICMPEQ
    do {
        "loop"
    } while (a == 42)

    // Generates IF_ICMPGT
    do {
        "loop"
    } while (a > 42)

    // Generates IF_ICMPGE
    do {
        "loop"
    } while (a >= 42)

    // Generates IF_ICMPLT
    do {
        "loop"
    } while (a < 42)

    // Generates IF_ICMPLE
    do {
        "loop"
    } while (a <= 42)
}

//0 ICONST_0
//1 ICONST_1
//1 IF_ICMPEQ
//1 IF_ICMPLE
//1 IF_ICMPLT
//1 IF_ICMPGE
//1 IF_ICMPGT
//5 IF
//0 GOTO