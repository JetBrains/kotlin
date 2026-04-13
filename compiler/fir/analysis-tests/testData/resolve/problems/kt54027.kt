// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-54027

// KT-54027: False "No cast needed" warning for sealed class instance returned from a lambda passed to generic map()

sealed class Parent {
    object Child1 : Parent()
    object Child2 : Parent()
}

class Observable<T>(private val value: T) {
    fun <R> map(f: (T) -> R): Observable<R> = Observable(f(value))
    fun startWithItem(item: T): Observable<T> = this
}

fun bug(): Observable<Parent> {
    return Observable(1).map {
        Parent.Child2 as Parent // False "No cast needed" warning; removing cast causes type mismatch
    }.startWithItem(Parent.Child1)
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, functionalType, integerLiteral,
lambdaLiteral, nestedClass, nullableType, objectDeclaration, primaryConstructor, propertyDeclaration, sealed,
thisExpression, typeParameter */
