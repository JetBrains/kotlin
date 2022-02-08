open class C(val x: Int)

class D : C {
    constructor() : super(
            {
                val s = ""
                <!UNRESOLVED_REFERENCE!>s<!>()
                <!FUNCTION_EXPECTED!>""<!>()
                42
            }())

    operator fun String.invoke() { }
}
