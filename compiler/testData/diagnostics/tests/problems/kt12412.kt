// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-12412
// WITH_STDLIB

// KT-12412: Cannot choose among the following candidates between two functions one has type T argument the other ()->T
fun <V> MutableList<V>.foo(v: V) {}
fun <V> MutableList<V>.foo(v: () -> V) {}

fun test(l: MutableList<Int>) {
    l.foo({ 1 })
}

fun test2() {
    val map = HashMap<String, String>()
    map.putIfMissing("test", "test")
}

fun test3() {
    val map = HashMap<String, String>()
    map.putIfMissing("test", { "test" })
}

fun <K : Any, V : Any> MutableMap<K, V>.putIfMissing(key: K, value: () -> V): V {
    val elem = this[key]
    if (elem == null) {
        val v = value()
        this[key] = v
        return v
    } else {
        return elem
    }
}

fun <K : Any, V : Any> MutableMap<K, V>.putIfMissing(key: K, value: V): V {
    val elem = this[key]
    if (elem == null) {
        this[key] = value
        return value
    } else {
        return elem
    }
}

/* GENERATED_FIR_TAGS: assignment, dnnType, equalityExpression, funWithExtensionReceiver, functionDeclaration,
functionalType, ifExpression, integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast,
stringLiteral, thisExpression, typeConstraint, typeParameter */
