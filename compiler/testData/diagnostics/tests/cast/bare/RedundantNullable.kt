// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface B<T>
class G<T>: B<T>

fun f(b: B<String>?) = b is G?<!REDUNDANT_NULLABLE!>?<!>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, isExpression, nullableType,
typeParameter */
