// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

class Context
class Extended

class Containing {
    context(Context) fun Extended.foo(obj: Any? = null) {}
}

fun box(): String {
    with (Containing()) {
        with (Context()) {
            Extended().foo()
        }
    }
    return "OK"
}