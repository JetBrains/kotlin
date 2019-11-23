val a = 2

fun main() {
    // Generates IF_ICMPEQ and GOTO
    while (!(a == 42)) {
        "loop"
    }

    // Generates IF_ICMPEQ and GOTO
    while (a != 42) {
        "loop"
    }

    // Generates IF_ICMPGT and GOTO
    while (!(a > 42)) {
        "loop"
    }

    // Generates IF_ICMPGE and GOTO
    while (!(a >= 42)) {
        "loop"
    }

    // Generates IF_ICMPLT and GOTO
    while (!(a < 42)) {
        "loop"
    }

    // Generates IF_ICMPLE and GOTO
    while (!(a <= 42)) {
        "loop"
    }
}

//0 ICONST_0
//0 ICONST_1
//2 IF_ICMPEQ
//1 IF_ICMPLE
//1 IF_ICMPLT
//1 IF_ICMPGE
//1 IF_ICMPGT
//6 IF
//6 GOTO