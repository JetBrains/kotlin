// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER -USELESS_CAST

class Inv<T>
class Out<out T>

fun <K> foo(y: K?) = Inv<Out<K>>()
fun <R> test(x: Inv<Out<R>>) {}

fun main() {
    test<Int>(foo(null)) // type mismatch
    test<Number>(foo(1 <!INTEGER_LITERAL_CAST_INSTEAD_OF_TO_CALL!>as Int<!>)) // type mismatch
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, integerLiteral, nullableType, out,
typeParameter */
