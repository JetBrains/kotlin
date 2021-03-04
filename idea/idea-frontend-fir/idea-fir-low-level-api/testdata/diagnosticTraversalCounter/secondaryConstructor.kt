class A(val x: Int = 10, val b: String) {
    constructor(i: Int) : this(x = 1, b = i.toString())
}