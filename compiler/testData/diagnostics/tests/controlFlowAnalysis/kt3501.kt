//KT-3501 Variable/parameter is highlighted as unused if it is used in member of local class

fun f(p: String) { // "p" is marked as unused
    class LocalClass {
        fun f() {
            <!UNUSED_EXPRESSION!>p<!>
        }
    }
}