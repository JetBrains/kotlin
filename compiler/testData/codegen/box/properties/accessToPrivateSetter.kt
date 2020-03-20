// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

class D {
    var foo = 1
        private set

    fun foo() {
        foo = 2
    }
}

fun box(): String {
   D().foo()
   return "OK"
}
