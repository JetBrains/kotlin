// LANGUAGE: +ContextParameters
// IGNORE_BACKEND: ANDROID

class Context
class Extended

class Containing {
    context(_: Context) fun Extended.foo(obj: Any? = null) {}
}

fun box(): String {
    with (Containing()) {
        with (Context()) {
            Extended().foo()
        }
    }
    return "OK"
}
