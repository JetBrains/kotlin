// FILE: A.kt
class A {
    private var privateVar = 22

    @Suppress("NOT_YET_SUPPORTED_IN_INLINE")
    internal inline fun internalGetValue(): Int {
        class LocalGet {
            fun localGet(): Int = privateVar
        }
        return LocalGet().localGet()
    }

    @Suppress("NOT_YET_SUPPORTED_IN_INLINE")
    internal inline fun internalSetValue(value: Int) {
        class LocalSet {
            fun localSet(n: Int) { privateVar = n }
        }
        LocalSet().localSet(value)
    }
}

// FILE: main.kt
fun box(): String {
    var result = 0
    A().run {
        result += internalGetValue()
        internalSetValue(20)
        result += internalGetValue()
    }
    if (result != 42) return result.toString()
    return "OK"
}
