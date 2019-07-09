class C() {
    constructor(x: Any?) : this() {
        if (x is Unit) return x
    }
}