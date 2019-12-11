open class C(val x: Int)

class D : C {
    constructor() : <!INAPPLICABLE_CANDIDATE!>super<!>(
            {
                val s = ""
                s()
                ""()
                42
            }())

    operator fun String.invoke() { }
}
