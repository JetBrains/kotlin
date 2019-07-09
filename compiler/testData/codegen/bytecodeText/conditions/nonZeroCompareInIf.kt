// Generates ICONST_1
val a = 1

fun main() {
    // Generates IF_ICMPNE and GOTO
    if (a == 42) {
        "then"
    } else {
        "else"
    }

    // Generates IF_ICMPLE and GOTO
    if (a > 42) {
        "then"
    } else {
        "else"
    }

    // Generates IF_ICMPLT and GOTO
    if (a >= 42) {
        "then"
    } else {
        "else"
    }

    // Generates IF_ICMPGE and GOTO
    if (a < 42) {
        "then"
    } else {
        "else"
    }

    // Generates IF_ICMPGT and GOTO
    if (a <= 42) {
        "then"
    } else {
        "else"
    }
}

//0 ICONST_0
//1 ICONST_1
//1 IF_ICMPNE
//1 IF_ICMPLE
//1 IF_ICMPLT
//1 IF_ICMPGE
//1 IF_ICMPGT
//5 IF
//5 GOTO