class A(val next: A? = null) {
    val x: String
    init {
        next?.x = "a"
        x = "b"
        this.x = "c"
        x = "d" // don't repeat the same diagnostic again with this receiver
        this.x = "e"

        next?.x = "f"
    }
}
