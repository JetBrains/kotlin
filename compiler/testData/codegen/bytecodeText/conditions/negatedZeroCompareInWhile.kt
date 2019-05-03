// Generates ICONST_1
val a = 1

fun main() {
    // Generates IFEQ and GOTO
    while (!(a == 0)) {
        "loop"
    }

    // Generates IFEQ and GOTO
    while (a != 0) {
        "loop"
    }

    // Generates IFGT and GOTO
    while (!(a > 0)) {
        "loop"
    }

    // Generates IFGE and GOTO
    while (!(a >= 0)) {
        "loop"
    }

    // Generates IFLT and GOTO
    while (!(a < 0)) {
        "loop"
    }

    // Generates IFLE and GOTO
    while (!(a <= 0)) {
        "loop"
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