// !CHECK_TYPE

fun <T> magic(): T = null!!

class Q {
    private fun <E> foo() =
        object {
            val prop: E = magic()
        }

    private var x = <!DEBUG_INFO_LEAKING_THIS!>foo<!><CharSequence>()
    private var y = <!DEBUG_INFO_LEAKING_THIS!>foo<!><String>()

    fun bar() {
        x = <!TYPE_MISMATCH!>y<!>
        x = foo<CharSequence>()
        y = foo<String>()

        x.prop.checkType { _<CharSequence>() }
        y.prop.checkType { _<String>() }
    }
}
