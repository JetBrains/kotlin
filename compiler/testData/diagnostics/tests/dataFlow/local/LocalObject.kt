fun foo(x: Any?) {
    if (x is String) {
        object : Base(<!DEBUG_INFO_AUTOCAST!>x<!>) {
            fun bar() = <!DEBUG_INFO_AUTOCAST!>x<!>.length
        }
    }
}

open class Base(s: String)