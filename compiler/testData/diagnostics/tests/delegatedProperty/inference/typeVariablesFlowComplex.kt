// FIR_IDENTICAL
// WITH_STDLIB
// !CHECK_TYPE
// ISSUE: KT-59529

import kotlin.reflect.KProperty

val ows: IgnoringParser = IgnoringParser()

class MyParser<T> {

    private val booleanLiteral: Parser<String> = TODO()

    val topLevel by ows + booleanLiteral

    fun main() {
        topLevel checkType { _<TransformParser<Pair<Unit, String>, String>>() }
    }

    operator fun <T, R> TransformParser<T, R>.provideDelegate(
        thisRef: MyParser<*>,
        property: KProperty<*>
    ): TransformParser<T, R> = TODO()

    operator fun <T, R> TransformParser<T, R>.getValue(
        thisRef: MyParser<*>,
        property: KProperty<*>
    ): TransformParser<T, R> = TODO()
}

abstract class Parser<out T>

class TransformParser<T, R> : Parser<R>()
class IgnoringParser : Parser<Unit>()

operator fun <T> IgnoringParser.plus(other: Parser<T>): TransformParser<Pair<Unit, T>, T> =
    TODO()
