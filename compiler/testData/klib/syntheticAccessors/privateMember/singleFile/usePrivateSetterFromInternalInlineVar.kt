class A {
    var privateSetterVarA = 0
        private set

    var privateSetterVarB = 0
        private set(value) {
            field = value * 2
        }

    internal inline var inlineVarA: Int
        get() = privateSetterVarA
        set(value) {
            privateSetterVarA = value
        }

    internal inline var inlineVarB: Int
        get() = privateSetterVarB
        set(value) {
            privateSetterVarB = value
        }
}

fun box(): String {
    var result = 0
    A().run {
        inlineVarA = 42
        result += inlineVarA
        inlineVarB = 21
        result += inlineVarB
    }
    if (result != 84) return result.toString()
    return "OK"
}
