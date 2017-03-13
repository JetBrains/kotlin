open class Variable {
    val lightVar: LightVariable = if (this is LightVariable) this else LightVariable()
}

class LightVariable() : Variable()

fun box(): String {
    Variable()
    return "OK"
}
