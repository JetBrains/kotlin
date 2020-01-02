class A(val next: A? = null) {
    val x: String
    init {
        next?.x = "a"
        this@A.x = "b"
        this.x = "c"
        x = "d" // don't repeat the same diagnostic again with this receiver
        this@A.x = "e"

        next?.x = "f"
    }
}
