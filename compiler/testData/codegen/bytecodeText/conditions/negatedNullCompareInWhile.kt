fun main(p: String?, p2: String?) {
    // Generates IFNULL and GOTO
    while (!(p == null)) {
        "loop"
    }

    // Generates IFNULL and GOTO
    while (p2 != null) {
        "loop"
    }
}

//0 ICONST_0
//0 ICONST_1
//0 ACONST_NULL
//2 IFNULL
//2 IF
//2 GOTO