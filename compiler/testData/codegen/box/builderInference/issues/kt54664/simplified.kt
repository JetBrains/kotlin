// ISSUE: KT-54664
// WITH_REFLECT

import kotlin.reflect.*

class Buildee<T> {
    fun yield(arg: T) { variable = arg }
    var variable: T = TargetType() as T
}

fun <T> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

class TargetType
class DifferentType

fun consume(arg: KMutableProperty0<DifferentType>) {}

fun box(): String {
    build {
        // K1: TYPE_MISMATCH (expected DifferentType, actual TargetType)
        yield(TargetType())
        variable = DifferentType()
        // K1: TYPE_MISMATCH (expected DifferentType, actual TargetType)
        consume(::variable)
    }
    return "OK"
}
