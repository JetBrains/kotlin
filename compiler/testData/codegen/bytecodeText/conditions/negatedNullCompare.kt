fun main(p: String?, p2: String?) {
    // Negated == null comparisons
    // Generates IFNULL and GOTO
    if (!(p == null)) {
        "then"
    } else {
        "else"
    }

    // Generates IFNULL and GOTO
    while (!(p == null)) {
        "loop"
    }

    // Generates IFNONNULL
    do {
        "loop"
    } while (!(p == null))

    // != null comparisons
    // Generates IFNULL and GOTO
    if (p2 != null) {
        "then"
    } else {
        "else"
    }

    // Generates IFNULL and GOTO
    while (p2 != null) {
        "loop"
    }

    // Generates IFNONNULL
    do {
        "loop"
    } while (p2 != null)
}

//0 ICONST_0
//0 ICONST_1
//0 ACONST_NULL
//4 IFNULL
//2 IFNONNULL
//6 IF
//4 GOTO
