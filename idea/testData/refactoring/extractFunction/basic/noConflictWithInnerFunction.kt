fun foo() {
    val a = 1
    // NEXT_SIBLING
    if (<selection>a > 0</selection>) {
        fun b(): Int { return 0 }
        println(b())
    }
}