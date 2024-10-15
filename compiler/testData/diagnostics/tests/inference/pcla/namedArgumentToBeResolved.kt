// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-72036

fun <Failure, Value> progressive(block: Thrower<Failure>.() -> Value) {}

abstract class Thrower<Failure> {
    abstract fun raise(r: Failure): Nothing
}

abstract class Owner {
    abstract fun <Error, A> recover(
        block: Thrower<Error>.() -> A,
        recover: (error: Error) -> A,
    ): A

    fun test() {
        progressive {
            recover<Double, _>(
                block = {
                    raise("")
                },
                recover = {
                    ""
                }
            )
        }
    }
}
