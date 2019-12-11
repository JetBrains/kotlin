// !WITH_NEW_INFERENCE
// !CHECK_TYPE
object Outer {
    private var x
        get() = object : CharSequence {
            override val length: Int
                get() = 0

            override fun get(index: Int): Char {
                <!UNRESOLVED_REFERENCE!>checkSubtype<!><CharSequence>(x)
                return ' '
            }

            override fun subSequence(startIndex: Int, endIndex: Int) = ""

            fun bar() {
            }
        }
        set(q) {
            <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><CharSequence>(x)
            y = q
            x = q
        }

    private var y = x

    fun foo() {
        x = y

        <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><CharSequence>(x)
        <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><CharSequence>(y)

        x.<!UNRESOLVED_REFERENCE!>bar<!>()
        y.<!UNRESOLVED_REFERENCE!>bar<!>()
    }
}
