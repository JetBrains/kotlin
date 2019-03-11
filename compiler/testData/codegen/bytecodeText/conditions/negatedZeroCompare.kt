// Generates ICONST_1
val a = 1

fun main() {
    // Negated == comparisons
    // Generates IFEQ and GOTO
    if (!(a == 0)) {
        "then"
    } else {
        "else"
    }

    // Generates IFEQ and GOTO
    while (!(a == 0)) {
        "loop"
    }

    // Generates IFNE
    do {
        "loop"
    } while (!(a == 0))

    // != comparisons
    // Generates IFEQ and GOTO
    if (a != 0) {
        "then"
    } else {
        "else"
    }

    // Generates IFEQ and GOTO
    while (a != 0) {
        "loop"
    }

    // Generates IFNE
    do {
        "loop"
    } while (a != 0)

    // Negated > comparisons
    // Generates IFGT and GOTO
    if (!(a > 0)) {
        "then"
    } else {
        "else"
    }

    // Generates IFGT and GOTO
    while (!(a > 0)) {
        "loop"
    }

    // Generates IFLE
    do {
        "loop"
    } while (!(a > 0))

    // Negated >= comparisons
    // Generates IFGE and GOTO
    if (!(a >= 0)) {
        "then"
    } else {
        "else"
    }

    // Generates IFGE and GOTO
    while (!(a >= 0)) {
        "loop"
    }

    // Generates IFLT
    do {
        "loop"
    } while (!(a >= 0))

    // Negated < comparisons
    // Generates IFLT and GOTO
    if (!(a < 0)) {
        "then"
    } else {
        "else"
    }

    // Generates IFLT and GOTO
    while (!(a < 0)) {
        "loop"
    }

    // Generates IFGE
    do {
        "loop"
    } while (!(a < 0))

    // Negated <= comparisons
    // Generates IFLE and GOTO
    if (!(a <= 0)) {
        "then"
    } else {
        "else"
    }

    // Generates IFLE and GOTO
    while (!(a <= 0)) {
        "loop"
    }

    // Generates IFGT
    do {
        "loop"
    } while (!(a <= 0))
}

//0 ICONST_0
//1 ICONST_1
//2 IFNE
//4 IFEQ
//3 IFLE
//3 IFLT
//3 IFGE
//3 IFGT
//18 IF
//12 GOTO