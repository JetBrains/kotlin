// IGNORE_BACKEND_FIR: JVM_IR
//KT-10934 compiler throws UninferredParameterTypeConstructor in when block that covers all types

class Parser<TInput, TValue>(val f: (TInput) -> Result<TInput, TValue>) {

    operator fun invoke(input: TInput): Result<TInput, TValue> = f(input)

    fun <TIntermediate, TValue2> mapJoin(
            selector: (TValue) -> Parser<TInput, TIntermediate>,
            projector: (TValue, TIntermediate) -> TValue2
    ): Parser<TInput, TValue2> {
        return Parser({ input ->
                          val res = this(input)
                          when (res) {
                              is Result.ParseError -> Result.ParseError(res.productionLabel, res.child, res.rest)
                              is Result.Value -> {
                                  val v = res.value
                                  val res2 = selector(v)(res.rest)
                                  when (res2) {
                                      is Result.ParseError -> Result.ParseError(res2.productionLabel, res2.child, res2.rest)
                                      is Result.Value -> Result.Value(projector(v, res2.value), res2.rest)
                                  }
                              }
                          }
                      })
    }
}

/** A parser can return one of two Results */
sealed class Result<TInput, TValue> {

    class Value<TInput, TValue>(val value: TValue, val rest: TInput) : Result<TInput, TValue>() {}

    class ParseError<TInput, TValue>(val productionLabel: String,
                                     val child: ParseError<TInput, *>?,
                                     val rest: TInput) : Result<TInput, TValue>() {}
}

fun box() = "OK"