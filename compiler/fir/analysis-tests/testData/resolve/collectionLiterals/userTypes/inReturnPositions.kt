// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        operator fun <K> of(vararg k: K) = MyList<K>()
    }
}

fun runLikeListInt(block: () -> MyList<Int>) = block()
fun <U> runLike(block: () -> MyList<U>) = block()
fun returnBoolean() = true
fun <V> myNullableList(): MyList<V>? = null
fun myNullableListInt() = myNullableList<Int>()

fun returnCollectionWithNoExpectedType() = <!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>
fun returnMyListInt(): MyList<Int> = [1, 2, 3]
fun <A> returnMyEmptyList(): MyList<A> = []
fun <B> returnMyListWithStrings(): MyList<B> = <!RETURN_TYPE_MISMATCH!>["1", "2", "3"]<!>
fun <C> returnMyListOf(c: C): MyList<C> = [c]
fun returnUnit() {
    return <!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>
}
fun returnWrappedInRunLike() = runLikeListInt { [1, 2, 3] }
fun returnWrappedInRunLikeWrongType() = runLikeListInt { <!RETURN_TYPE_MISMATCH!>["1", "2", "3"]<!> }
fun returnWrappedInRunLikeWrongExpectedType(): MyList<String> = <!RETURN_TYPE_MISMATCH!>runLikeListInt { [1, 2, 3] }<!>
fun returnWrappedInGenericRunLike() = runLike { [1, 2, 3] }
fun returnWrappedInGenericRunLikeWithExpectedType(): MyList<Int> = runLike { [1, 2, 3] }
fun returnWrappedInGenericRunLikeWithWrongExpectedType(): MyList<String> = runLike { <!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3]<!> }
fun <D> returnWrappedInGenericRunLikeWithTypeParameter(d: D) = runLike { [d, d, d] }
fun <E> returnWrappedInGenericRunLikeWithTypeParameterAndExpectedType(e: E): MyList<E> = runLike { [e, e, e] }

val property: MyList<Int> get() = [1, 2, 3]
val propertyWithWrongType: MyList<String> get() = <!RETURN_TYPE_MISMATCH!>[1, 2, 3]<!>
val <F> F.genericProperty: MyList<F> get() = []
val <G> G.genericPropertyWithStrings: MyList<G> get() = <!RETURN_TYPE_MISMATCH!>["1", "2", "3"]<!>

fun returnIfElse(): MyList<Any> = if (returnBoolean()) [1, 2, 3] else ["1", "2", "3"]
fun returnIfElseWithWrongExpectedType(): MyList<Int> = if (returnBoolean()) [1, 2, 3] else <!ARGUMENT_TYPE_MISMATCH!>["1", "2", "3"]<!>
fun <H> returnIfElseWithGenericExpectedType(h: H): MyList<H> = if (returnBoolean()) [] else [h]
fun returnIfElseWithRunLike(): MyList<Int> = if (returnBoolean()) runLike { [1, 2, 3] } else runLikeListInt { [] }

fun returnInElvis(): MyList<Int> = myNullableList() ?: []
fun returnInElvisNoExpectedType() = <!CANNOT_INFER_PARAMETER_TYPE!>myNullableList<!>() ?: <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
fun returnInElvisListInt() = myNullableListInt() ?: []

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, elvisExpression, functionDeclaration, functionalType, getter,
ifExpression, integerLiteral, lambdaLiteral, nullableType, objectDeclaration, operator, propertyDeclaration,
propertyWithExtensionReceiver, stringLiteral, typeParameter, vararg */
