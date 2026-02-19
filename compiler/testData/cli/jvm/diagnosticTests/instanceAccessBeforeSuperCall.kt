class A {
    constructor(x: Int = getSomeInt(), other: A = this, header: String = keker) {}
    fun getSomeInt() = 10
    var keker = "test"
}

class B(other: B = this)

class C() {
    constructor(x: Int) : this({
        val a = 10
        this
    }) {}
}

class D {
    var a = 20
    constructor() {
        this.a = 10
    }
}