fun main(p: String?, p2: String?) {
    // Generates IFNONNULL
    do {
        "loop"
    } while (!(p == null))

    // Generates IFNONNULL
    do {
        "loop"
    } while (p2 != null)
}

//0 ICONST_0
//0 ICONST_1
//0 ACONST_NULL
//2 IFNONNULL
//2 IF
//0 GOTO