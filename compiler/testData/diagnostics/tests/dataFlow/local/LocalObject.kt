fun foo(x: Any?) {
    if (x is String) {
        object : Base(<!DEBUG_INFO_SMARTCAST!>x<!>) {
            fun bar() = <!DEBUG_INFO_SMARTCAST!>x<!>.length
        }
    }
}

open class Base(<!UNUSED_PARAMETER!>s<!>: String)