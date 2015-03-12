class A {
    val x: Int
    val y: Int
    constructor(x: Int, y: Int) {
        this.x = x
        this.y = y
    }
    constructor(other: A) {
        x = other.x
        y = other.y
    }
}
class A1(val x: Int, val y: Int) {
    constructor(other: A1): this(other.x, other.y) {}
}
