fun <K> materialize(): K = null!!

open class A1(val x: String)
class B1 : A1(materialize())

open class A2(val x: Int)
class B2 : A2(1 + 1)

open class A3(x: String, y: String = "") {
    constructor(x: String, b: Boolean = true) : this(x, x)
}

class B3_1 : <!AMBIGUITY!>A3<!>("")
class B3_2 : A3("", "asas")
class B3_3 : A3("", true)
class B3_4 : <!INAPPLICABLE_CANDIDATE!>A3<!>("", Unit)

open class A4(val x: Byte)
class B4 : A4( 1 + 1)

open class A5 {
    constructor(x: Byte)
    constructor(x: Short)
}

class B5_1 : A5(1 + 1)
class B5_2 : A5(100 * 2)