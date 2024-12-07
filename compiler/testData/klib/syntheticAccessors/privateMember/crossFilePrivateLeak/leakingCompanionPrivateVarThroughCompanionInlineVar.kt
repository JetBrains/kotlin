// FILE: A.kt
class A {
    companion object {
        private var privateVar = 12

        internal inline var inlineCompanionVar: Int
            get() = privateVar
            set(value) {
                privateVar = value
            }
    }
}

// FILE: main.kt
fun box(): String {
    var result = 0

    result += A.inlineCompanionVar
    A.inlineCompanionVar = 1
    result += A.inlineCompanionVar
    if (result != 13) return result.toString()
    return "OK"
}
