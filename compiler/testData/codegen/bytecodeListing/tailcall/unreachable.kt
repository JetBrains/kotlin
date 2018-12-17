// COMMON_COROUTINES_TEST
// WITH_RUNTIME

suspend fun empty() {}
suspend fun withoutReturn() {
    empty()
}

suspend fun twoReturns() {
    return empty()
    return empty()
}

suspend fun notTailCall() {
    empty()
    return empty()
    empty()
}

suspend fun lambdaAsParameter(c: suspend () -> Unit) {
    c()
}

suspend fun lambdaAsParameterNotTailCall(c: suspend () -> Unit) {
    c()
    return c()
    c()
}

suspend fun lambdaAsParameterReturn(c: suspend () -> Unit) {
    return c()
    c()
}

suspend fun returnsInt() = 42
suspend fun callsIntNotTailCall() {
    returnsInt()
    return
    empty()
}

suspend fun multipleExitPoints(b: Boolean) {
    if (b) empty() else withoutReturn()
    return
    empty()
}

suspend fun multipleExitPointsNotTailCall(b: Boolean) {
    if (b) empty() else returnsInt()
    return
    empty()
}

fun ordinary() = 1
inline fun ordinaryInline() {
    ordinary()
}

suspend fun multipleExitPointsWithOrdinaryInline(b: Boolean) {
    if (b) empty() else ordinaryInline()
    return
    empty()
}

suspend fun multipleExitPointsWhen(i: Int) {
    when (i) {
        1 -> empty()
        2 -> twoReturns()
        3 -> withoutReturn()
        else -> lambdaAsParameter {}
    }
    return
    empty()
}

suspend fun <T> generic(): T = TODO()
suspend fun useGenericReturningUnit() {
    generic<Unit>()
    return
    empty()
}

class Generic<T> {
    suspend fun foo(): T = TODO()
}

suspend fun useGenericClass(g: Generic<Unit>) {
    g.foo()
    return
    empty()
}

suspend fun <T> genericInferType(c: () -> T): T = TODO()
suspend fun useGenericInferType() {
    genericInferType {}
    return
    empty()
}

suspend fun nullableUnit(): Unit? = null
suspend fun useNullableUnit() {
    nullableUnit()
    return
    empty()
}

suspend fun useRunRunRunRunRun() {
    run {
        run {
            run {
                run {
                    run {
                        empty()
                    }
                }
            }
        }
    }
    return
    empty()
}
