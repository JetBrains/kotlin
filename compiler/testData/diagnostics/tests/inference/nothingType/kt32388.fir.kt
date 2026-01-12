// RUN_PIPELINE_TILL: BACKEND

fun <A, B> Either<A, B>.recover(f: (A) -> B): Either<A, B> = <!WHEN_ON_SEALED!>when (this) {
    is Either.Left -> f(this.a).right()
    is Either.Right -> this
}<!>

fun <A> A.right(): Either<Nothing, A> = Either.Right(this)

sealed class Either<out A, out B> {
    class Left<out A> constructor(val a: A) : Either<A, Nothing>()
    class Right<out B> constructor(val b: B) : Either<Nothing, B>()
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, isExpression,
nestedClass, nullableType, out, primaryConstructor, propertyDeclaration, sealed, smartcast, thisExpression,
typeParameter, whenExpression, whenWithSubject */
