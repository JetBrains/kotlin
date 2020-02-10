// !WITH_NEW_INFERENCE

fun <A, B> Either<A, B>.recover(f: (A) -> B): Either<A, B> = when (this) {
    is Either.Left -> f(this.a).right()
    is Either.Right -> this
}

fun <A> A.right(): Either<Nothing, A> = Either.Right(this)

sealed class Either<out A, out B> {
    class Left<out A> constructor(val a: A) : Either<A, Nothing>()
    class Right<out B> constructor(val b: B) : Either<Nothing, B>()
}
