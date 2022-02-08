// !CHECK_TYPE
object Outer {
    private var x
        get() = object : CharSequence {
            override val length: Int
                get() = 0

            override fun get(index: Int): Char {
                checkSubtype<CharSequence>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
                return ' '
            }

            override fun subSequence(startIndex: Int, endIndex: Int) = ""

            fun bar() {
            }
        }
        set(q) {
            checkSubtype<CharSequence>(x)
            y = q
            x = q
        }

    private var y = x

    fun foo() {
        x = y

        checkSubtype<CharSequence>(x)
        checkSubtype<CharSequence>(y)

        x.bar()
        y.bar()
    }
}
