open class C(val x: Int)

class D : C {
    constructor() : super(
            {
                val s = ""
                <!UNRESOLVED_REFERENCE!>s<!>()
                <!UNRESOLVED_REFERENCE!>""()<!>
                42
            }())

    operator fun String.invoke() { }
}
