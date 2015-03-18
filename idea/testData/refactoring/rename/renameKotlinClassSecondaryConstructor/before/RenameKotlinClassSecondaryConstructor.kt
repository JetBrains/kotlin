open class X {
    constructor(x: Int) {}
}

class Y1 : X(1)
class Y2 : X {
    constructor(): super(1) {}
}

val x = X(1)
