// Generates ICONST_1
val a = 1

fun main() {
    // == comparisons
    // Generates IFNE and GOTO
    if (a == 0) {
        "then"
    } else {
        "else"
    }

    // Generates IFNE and GOTO
    while (a == 0) {
        "loop"
    }

    // Generates IFEQ
    do {
        "loop"
    } while (a == 0)

    // > comparisons
    // Generates IFLE and GOTO
    if (a > 0) {
        "then"
    } else {
        "else"
    }

    // Generates IFLE and GOTO
    while (a > 0) {
        "loop"
    }

    // Generates IFGT
    do {
        "loop"
    } while (a > 0)

    // >= comparisons
    // Generates IFLT and GOTO
    if (a >= 0) {
        "then"
    } else {
        "else"
    }

    // Generates IFLT and GOTO
    while (a >= 0) {
        "loop"
    }

    // Generates IFGE
    do {
        "loop"
    } while (a >= 0)

    // < comparisons
    // Generates IFGE and GOTO
    if (a < 0) {
        "then"
    } else {
        "else"
    }

    // Generates IFGE and GOTO
    while (a < 0) {
        "loop"
    }

    // Generates IFLT
    do {
        "loop"
    } while (a < 0)

    // <= comparisons
    // Generates IFGT and GOTO
    if (a <= 0) {
        "then"
    } else {
        "else"
    }

    // Generates IFGT and GOTO
    while (a <= 0) {
        "loop"
    }

    // Generates IFLE
    do {
        "loop"
    } while (a <= 0)
}

//0 ICONST_0
//1 ICONST_1
//2 IFNE
//1 IFEQ
//3 IFLE
//3 IFLT
//3 IFGE
//3 IFGT
//15 IF
//10 GOTO
