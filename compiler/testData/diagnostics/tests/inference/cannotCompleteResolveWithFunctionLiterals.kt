// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONFLICTING_JVM_DECLARATIONS
package f

class In<in K>

fun <R> h(f: (Boolean) -> R) = 1
fun <R> h(f: (String) -> R) = 2

fun <R, K> f(f: (Boolean) -> R, vararg x: In<K>) = 1
fun <R, K> f(f: (String) -> R, vararg x: In<K>) = 2

fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>h<!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>i<!> -> getAnswer() }
    <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING, NONE_APPLICABLE!>f<!>({ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>i<!> -> getAnswer() }, In<String>(), In<Int>())
}

fun getAnswer() = 42

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, in, integerLiteral, lambdaLiteral,
nullableType, typeParameter, vararg */
