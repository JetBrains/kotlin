object A1() {
    constructor(x: Int = "", y: Int) : this() {
        x + y
    }
}

object A2 public constructor(private val prop: Int) {
    constructor(x: Int = "", y: Int) : this(x * y) {
        x + y
    }
}

val x = object (val prop: Int) {
    <!CONSTRUCTOR_IN_OBJECT, CONSTRUCTOR_IN_OBJECT!>constructor()<!> : <!UNRESOLVED_REFERENCE!>this<!>(1) {
        val x = 1
        x * x
    }
}

class A3 {
    companion object B(val prop: Int) {
        public constructor() : this(2)
    }
}