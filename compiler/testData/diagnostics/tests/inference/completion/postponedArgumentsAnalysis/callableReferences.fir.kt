// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER -CAST_NEVER_SUCCEEDS -UNUSED_VARIABLE

class Foo<T>
class P<K, T>(x: K, y: T)

val Foo<Int>.bar: Foo<Int> get() = this

fun <T> Foo<T>.bar(x: String) = null as Foo<Int>

fun main() {
    val x: P<String, Foo<Int>.() -> Foo<Int>> = P("", Foo<Int>::bar)
}

/* GENERATED_FIR_TAGS: asExpression, callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration,
getter, localProperty, nullableType, primaryConstructor, propertyDeclaration, propertyWithExtensionReceiver,
stringLiteral, thisExpression, typeParameter */
