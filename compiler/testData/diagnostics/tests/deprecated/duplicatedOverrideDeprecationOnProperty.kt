// FIR_IDENTICAL
// ISSUE: KT-51893

interface A {
    @Deprecated("")
    val a: String

    @Deprecated("")
    val b: String

    @Deprecated("")
    var c: String

    @Deprecated("")
    var d: String

    @Deprecated("")
    var e: String

    @Deprecated("")
    var h: String
}

object B : A {
    override val <!OVERRIDE_DEPRECATION!>a<!>: String = ""

    override val <!OVERRIDE_DEPRECATION!>b<!>: String
        get() = ""

    override var <!OVERRIDE_DEPRECATION!>c<!>: String = ""

    override var <!OVERRIDE_DEPRECATION!>d<!>: String = ""
        get() = field

    override var <!OVERRIDE_DEPRECATION!>e<!>: String = ""
        set(value) {
            field = value
        }

    override var <!OVERRIDE_DEPRECATION!>h<!>: String = ""
        get() = field
        set(value) {
            field = value
        }
}
