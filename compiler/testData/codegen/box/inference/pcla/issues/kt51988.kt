// WITH_STDLIB

import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
fun <AB1, BB1> build(block: BuilderScope<AB1>.() -> BB1): ResultProvider<AB1, BB1> = object : ResultProvider<AB1, BB1> {
    override fun provideResult(): AB1 = "OK" as AB1
}

@OptIn(ExperimentalTypeInference::class)
fun <AB2, BB2> build2(block: BuilderScope<AB2>.() -> BB2): ResultProvider<AB2, BB2> = object : ResultProvider<AB2, BB2> {
    override fun provideResult(): AB2 = "OK" as AB2
}

interface BuilderScope<BS> {
    fun <B1> getResult(result: ResultProvider<BS, B1>): B1
    fun <B2> getResult2(result: ResultProvider<BS, B2>): B2
}

interface ResultProvider<AR, BR> {
    fun provideResult(): AR
}

val resultProvider: ResultProvider<Any, Any> = object : ResultProvider<Any, Any> {
    override fun provideResult(): Any = "NOK"
}

val result = build {
    getResult(build2 {
        getResult2(resultProvider)
    })
}

fun box(): String {
    return result.provideResult().toString()
}
