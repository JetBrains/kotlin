// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-32486

// KT-32486: NI marks upcast in lambda return as redundant, but removing it causes type error in chained call

sealed class Base {
    object Child1 : Base()
    object Child2 : Base()
}

class Container<T>(val value: T) {
    fun merge(other: Container<T>): Container<T> = Container(other.value)
}

fun <T, R> Container<T>.fmap(f: (T) -> R): Container<R> = Container(f(value))

fun test() {
    val b: Container<Base> = Container(Base.Child1)
    // Without 'as Base', type inference gives Container<Base.Child2> and merge fails
    // NI incorrectly marks 'as Base' as redundant
    val result = Container(0)
        .fmap { Base.Child2 as Base }
        .merge(b)
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
integerLiteral, lambdaLiteral, localProperty, nestedClass, nullableType, objectDeclaration, primaryConstructor,
propertyDeclaration, sealed, typeParameter */
