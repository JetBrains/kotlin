// Generates ICONST_1
val a = 1

fun main() {
    // Generates IFEQ and GOTO
    if (!(a == 0)) {
        "then"
    } else {
        "else"
    }

    // Generates IFEQ and GOTO
    if (a != 0) {
        "then"
    } else {
        "else"
    }

    // Generates IFGT and GOTO
    if (!(a > 0)) {
        "then"
    } else {
        "else"
    }

    // Generates IFGE and GOTO
    if (!(a >= 0)) {
        "then"
    } else {
        "else"
    }

    // Generates IFLT and GOTO
    if (!(a < 0)) {
        "then"
    } else {
        "else"
    }

    // Generates IFLE and GOTO
    if (!(a <= 0)) {
        "then"
    } else {
        "else"
    }
}

//0 ICONST_0
//1 ICONST_1
//2 IFEQ
//1 IFLE
//1 IFLT
//1 IFGE
//1 IFGT
//6 IF
//6 GOTO