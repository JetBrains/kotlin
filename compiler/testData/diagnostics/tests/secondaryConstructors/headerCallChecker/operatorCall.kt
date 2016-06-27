open class C(val x: Int)

class D : C {
    constructor() : super(
            {
                val s = ""
                <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>s<!>()
                ""() // TODO: see KT-12875
                42
            }())

    operator fun String.invoke() { }
}
