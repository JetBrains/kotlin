fun main(p: String?, p2: String?) {
    // Generates IFNULL and GOTO
    if (!(p == null)) {
        "then"
    } else {
        "else"
    }

    // Generates IFNULL and GOTO
    if (p2 != null) {
        "then"
    } else {
        "else"
    }
}

//0 ICONST_0
//0 ICONST_1
//0 ACONST_NULL
//2 IFNULL
//2 IF
//2 GOTO