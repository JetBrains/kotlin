// TARGET_BACKEND: WASM

fun topFun(x: Int) = x + 1

fun box(): String {
    val lambda: (Int) -> Int = { it * 2 }
    if (lambda::class.qualifiedName != null) return "Fail1: ${lambda::class.qualifiedName}"
    if (lambda::class.simpleName != null) return "Fail2: ${lambda::class.simpleName}"

    val suspendLambda: suspend () -> Int = { 1 }
    if (suspendLambda::class.qualifiedName != null) return "Fail3: ${suspendLambda::class.qualifiedName}"
    if (suspendLambda::class.simpleName != null) return "Fail4: ${suspendLambda::class.simpleName}"

    val reference: (Int) -> Int = ::topFun
    if (reference::class.qualifiedName != null) return "Fail5: ${reference::class.qualifiedName}"
    if (reference::class.simpleName != null) return "Fail6: ${reference::class.simpleName}"

    // Sanity check: a named class keeps its simple name
    class Local
    if (Local::class.simpleName != "Local") return "Fail7"
    if (NonLocal::class.qualifiedName != "NonLocal") return "Fail8"

    return "OK"
}

class NonLocal
