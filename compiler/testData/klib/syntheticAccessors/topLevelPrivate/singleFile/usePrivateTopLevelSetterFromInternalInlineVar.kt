var privateSetterVar = 12
    private set

internal inline var inlineVar: Int
    get() = privateSetterVar
    set(value) {
        privateSetterVar = value
    }

fun box(): String {
    var result = 0
    result += inlineVar
    inlineVar = 3
    result += inlineVar
    if (result != 15) return result.toString()
    return "OK"
}
