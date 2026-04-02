// RUN_PIPELINE_TILL: BACKEND
interface Map<K, out V>
interface MutableMap<K, V>: Map<K, V> {
  operator fun set(k: K, v: V)
}

fun p(p: Map<String, Int>) {
    if (p is MutableMap<String, Int>) {
        p[""] = 1
    }
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, ifExpression, integerLiteral, interfaceDeclaration, isExpression,
nullableType, operator, out, smartcast, stringLiteral, typeParameter */
