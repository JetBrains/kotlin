open class A {
    open val valProp: Int = -1
    open var varProp: Int = -1
    open var isProp: Int = -1
}

fun box(): String {
    val j = J()
    val a: A = j

    if (j.valProp != 123) return "fail 1"
    if (a.valProp != 123) return "fail 2"

    j.varProp = -1
    if (!j.okField) return "fail 3"
    j.okField = false

    a.varProp = -1
    if (!j.okField) return "fail 4"
    j.okField = false

    if (j.varProp != 456) return "fail 5"
    if (a.varProp != 456) return "fail 6"

    j.isProp = -1
    if (!j.okField) return "fail 7"
    j.okField = false

    a.isProp = -1
    if (!j.okField) return "fail 8"
    j.okField = false

    if (j.isProp != 789) return "fail 9"
    if (a.isProp != 789) return "fail 10"

    return "OK"
}
