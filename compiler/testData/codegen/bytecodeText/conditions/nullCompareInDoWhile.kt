fun main(p: String?) {
    // Generates IFNULL
    do {
        "loop"
    } while (p == null)
}

//0 ICONST_0
//0 ICONST_1
//0 ACONST_NULL
//1 IFNULL
//1 IF
//0 GOTO