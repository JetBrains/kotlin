fun foo(x: Any?) {
    if (x is String) {
        object : Base(x) {
            fun bar() = x.length
        }
    }
}

open class Base(s: String)