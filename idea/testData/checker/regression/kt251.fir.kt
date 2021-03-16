class A() {
    var x: Int = 0
        get() = "s"
        set(value: String) {
            field = value
        }
    val y: Int
        get(): String = "s"
    val z: Int
        get() {
            return "s"
        }

    var a: Any = 1
        set(v: String) {
            field = v
        }
    val b: Int
        get(): Any = "s"
    val c: Int
        get() {
            return 1
        }
    val d = 1
        get() {
            return field
        }
    val e = 1
        get(): String {
            return field
        }

}
