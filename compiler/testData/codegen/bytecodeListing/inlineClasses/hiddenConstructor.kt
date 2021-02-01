inline class Z(val x: Int)

class Test1(val z: Z)

class Test2(val x: String) {
    constructor(z: Z) : this(z.toString())
}

class Test3(val z: Z = Z(0))

class Test4(val x: String) {
    constructor(z: Z = Z(0)) : this(z.toString())
}