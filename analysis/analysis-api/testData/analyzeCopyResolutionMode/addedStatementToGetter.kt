// MODULE: original
val prop: Int
    get() {
        return 0
    }
// MODULE: copy
val prop: Int
    get() {
        println()
        return 0
    }