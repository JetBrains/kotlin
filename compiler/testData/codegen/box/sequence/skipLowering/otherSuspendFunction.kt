// WITH_STDLIB

suspend fun <T> SequenceScope<T>.yieldIfNotNull(t: T?) {
    if (t != null) yield(t)
}

sealed class MockLHS {
    class Type(val isClass: Boolean, val isStatic: Boolean) : MockLHS()
    class Expression(val isObjectQualifier: Boolean) : MockLHS()
}

fun tryResolve(name: String, shouldSucceed: Boolean): String? {
    return if (shouldSucceed) name else null
}

fun buildMockSequence(lhs: MockLHS): Sequence<String> {
    return sequence {
        when (lhs) {
            is MockLHS.Type -> {
                if (!lhs.isClass) {
                    return@sequence
                }

                if (lhs.isStatic) {
                    yieldIfNotNull(tryResolve("static_scope", true))
                }

                yieldIfNotNull(tryResolve("receiver_scope", false))
            }
            is MockLHS.Expression -> {
                yieldIfNotNull(tryResolve("bound_scope", true))

                if (lhs.isObjectQualifier) {
                    yieldIfNotNull(tryResolve("object_scope", true))
                }
            }
        }
    }
}

fun box(): String {
    val emptyResult = buildMockSequence(MockLHS.Type(isClass = false, isStatic = true)).toList()
    if (emptyResult.isNotEmpty()) return "Failed early return: $emptyResult"

    val typeResult = buildMockSequence(MockLHS.Type(isClass = true, isStatic = true)).toList()
    if (typeResult != listOf("static_scope")) return "Failed type branch: $typeResult"

    val exprResult = buildMockSequence(MockLHS.Expression(isObjectQualifier = true)).toList()
    if (exprResult != listOf("bound_scope", "object_scope")) return "Failed expression branch: $exprResult"

    return "OK"
}
