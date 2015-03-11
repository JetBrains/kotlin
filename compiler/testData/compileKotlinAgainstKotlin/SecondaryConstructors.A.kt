open class A {
    val prop: String
    constructor(x1: String, x2: String = "abc") {
        prop = "$x1#$x2"
    }
    constructor(x1: Long) {
        prop = "$x1"
    }
}
