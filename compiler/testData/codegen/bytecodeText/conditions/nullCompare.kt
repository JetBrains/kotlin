fun main(p: String?) {
    // Generates IFNONNULL and GOTO
    if (p == null) {
        "then"
    } else {
        "else"
    }

    // Generates IFNONNULL and GOTO
    while (p == null) {
        "loop"
    }

    // Generates IFNULL
    do {
        "loop"
    } while (p == null)
}

//0 ICONST_0
//0 ICONST_1
//0 ACONST_NULL
//2 IFNONNULL
//1 IFNULL
//3 IF
//2 GOTO
