// WITH_STDLIB
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

fun foo(s: String?): String {
    s?.let { it ->
        return it
    }
    return ""
}

fun bar(s: String?, t: String?): String {
    s?.let {
        t?.let {
            return it
        }
    }
    return ""
}

val String?.baz: String
    get() {
        this?.let {
            return it
        }
        return ""
    }
