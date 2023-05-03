object A1<!CONSTRUCTOR_IN_OBJECT!>()<!> {
    <!CONSTRUCTOR_IN_OBJECT!>constructor(x: Int = "", y: Int)<!> : <!UNRESOLVED_REFERENCE!>this<!>() {
        x + y
    }
}

object A2 public <!CONSTRUCTOR_IN_OBJECT!>constructor(private val prop: Int)<!> {
    <!CONSTRUCTOR_IN_OBJECT!>constructor(x: Int = "", y: Int)<!> : <!NONE_APPLICABLE!>this<!>(x * y) {
        x + y
    }
}

val x = object <!CONSTRUCTOR_IN_OBJECT!>(val prop: Int)<!> {
    <!CONSTRUCTOR_IN_OBJECT!>constructor()<!> : this(1) {
        val x = 1
        x * x
    }
}

class A3 {
    companion object B<!CONSTRUCTOR_IN_OBJECT!>(val prop: Int)<!> {
        public <!CONSTRUCTOR_IN_OBJECT!>constructor()<!> : <!NONE_APPLICABLE!>this<!>(2)
    }
}
