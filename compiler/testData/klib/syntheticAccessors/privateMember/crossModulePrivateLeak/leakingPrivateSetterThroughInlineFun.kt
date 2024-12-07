// KT-72862: <missing declarations>
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// JVM_ABI_K1_K2_DIFF
// MODULE: lib
// FILE: A.kt
class A {
    var a: Int = 0
        private set

    var b: Int = 0
        private set(value) {
            field = value * 2
        }


    internal inline fun internalInlineSetA() {
        a = 42
    }

    internal inline fun internalInlineSetB() {
        b = 21
    }
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    A().run {
        internalInlineSetA()
        internalInlineSetB()
        result += a
        result += b
    }
    if (result != 84) return result.toString()
    return "OK"
}
