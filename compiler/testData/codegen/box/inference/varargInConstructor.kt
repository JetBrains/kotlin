// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +UnitConversionsOnArbitraryExpressions, +InferThrowableTypeParameterToUpperBound

class Foo(vararg val strings: () -> String, var result: String = "primary") {
    constructor(unit: () -> Unit) : this() {
        result = "secondary"
    }
}

fun box(): String {
    val trailingString = Foo { "" }.result
    if (trailingString != "secondary") return "fail 1: $trailingString"

    val trailingUnit = Foo { Unit }.result
    if (trailingUnit != "secondary") return "fail 2: $trailingUnit"

    val regularString = Foo({ "" }).result
    if (regularString != "primary") return "fail 3: $regularString"

    val regularUnit = Foo({ Unit }).result
    if (regularUnit != "secondary") return "fail 4: $regularUnit"

    return "OK"
}
