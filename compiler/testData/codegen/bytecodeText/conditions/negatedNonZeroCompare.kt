// Generates ICONST_1
val a = 1

fun main() {
    // Negated == comparisons
    // Generates IF_ICMPEQ and GOTO
    if (!(a == 42)) {
        "then"
    } else {
        "else"
    }

    // Generates IF_ICMPEQ and GOTO
    while (!(a == 42)) {
        "loop"
    }

    // Generates IF_ICMPNE
    do {
        "loop"
    } while (!(a == 42))

    // != comparisons
    // Generates IF_ICMPEQ and GOTO
    if (a != 42) {
        "then"
    } else {
        "else"
    }

    // Generates IF_ICMPEQ and GOTO
    while (a != 42) {
        "loop"
    }

    // Generates IF_ICMPNE
    do {
        "loop"
    } while (a != 42)

    // Negated > comparisons
    // Generates IF_ICMPGT and GOTO
    if (!(a > 42)) {
        "then"
    } else {
        "else"
    }

    // Generates IF_ICMPGT and GOTO
    while (!(a > 42)) {
        "loop"
    }

    // Generates IF_ICMPLE
    do {
        "loop"
    } while (!(a > 42))

    // Negated >= comparisons
    // Generates IF_ICMPGE and GOTO
    if (!(a >= 42)) {
        "then"
    } else {
        "else"
    }

    // Generates IF_ICMPGE and GOTO
    while (!(a >= 42)) {
        "loop"
    }

    // Generates IF_ICMPLT
    do {
        "loop"
    } while (!(a >= 42))

    // Negated < comparisons
    // Generates IF_ICMPLT and GOTO
    if (!(a < 42)) {
        "then"
    } else {
        "else"
    }

    // Generates IF_ICMPLT and GOTO
    while (!(a < 42)) {
        "loop"
    }

    // Generates IF_ICMPGE
    do {
        "loop"
    } while (!(a < 42))

    // Negated <= comparisons
    // Generates IF_ICMPLE and GOTO
    if (!(a <= 42)) {
        "then"
    } else {
        "else"
    }

    // Generates IF_ICMPLE and GOTO
    while (!(a <= 42)) {
        "loop"
    }

    // Generates IF_ICMPGT
    do {
        "loop"
    } while (!(a <= 42))
}

//0 ICONST_0
//1 ICONST_1
//2 IF_ICMPNE
//4 IF_ICMPEQ
//3 IF_ICMPLE
//3 IF_ICMPLT
//3 IF_ICMPGE
//3 IF_ICMPGT
//18 IF
//12 GOTO