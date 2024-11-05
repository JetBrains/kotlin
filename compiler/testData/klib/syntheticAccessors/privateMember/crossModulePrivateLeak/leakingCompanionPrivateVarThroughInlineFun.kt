// KT-72862: <missing declarations>
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// MODULE: lib
// FILE: A.kt
class A {
    companion object{
        private var privateVar = 12
    }

    internal inline fun customSetVar(value: Int) {
        privateVar = value
    }

    internal inline fun customGetVar(): Int = privateVar
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    A().run {
        result += customGetVar()
        customSetVar(1)
        result += customGetVar()
    }
    if (result != 13) return result.toString()
    return "OK"
}
