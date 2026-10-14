// The declarations of the used annotations are at the bottom of the file.
// Everything has to live in a single file as this test data doesn't support the `// FILE:` directive,
// hence the `test` package: some of the annotations below are referenced by their fully qualified names.
package test

import test.E.E1
import kotlin.reflect.KClass

@Arrays(
    [1, 2, 3],
    [1L],
    [],
    [2.2],
    ['a'],
    [true, false]
)
class WithArrays

@Arrays(
    intArrayOf(1, 2, 3),
    longArrayOf(1L),
    floatArrayOf(),
    doubleArrayOf(2.2),
    charArrayOf('a'),
    booleanArrayOf(true, false),
)
class WithExplicitArrays

@ClassLiteral(
    WithClassLiteral::class,
    Boolean::class,
)
class WithClassLiteral

@EnumLiteral(E1, E.E2, e3 = test.E.E2)
class WithEnumLiteral

@VarArg(1)
class OneVararg

@VarArg(1, 2)
class TwoVararg

@VarArg(1, 2, VarArg.CONSTANT)
class ThreeVararg

@VarArg(*[1, 2, VarArg.CONSTANT, 4])
class SpreadVararg

@Outer("value", nested = Nested(0, "nested value"))
class WithNested

@Outer("value", test.Nested(1, "nested value"))
class WithQualifiedNested

@Simple(test.Simple.Companion.CONST1)
class Qualified

@Simple(test.Simple.Companion.CONST1 + Simple.CONST2)
class Sum

@Simple(-test.Simple.Companion.CONST1)
class Negative

@Simple(- - -test.Simple.Companion.CONST1)
class Negative2

@Simple(-(-test.Simple.Companion.CONST1))
class Positive

@Simple(
    12,
    12L,
    12,

    3.3,
    3.3F,

    'a',

    true,
    false
)
class WithSimple

@Simple(
    12,
    12L,
    12,

    d = 3.3,
    f = 3.3F,

    c = 'a',

    b1 = true,
    b2 = false
)
class WithNamedSimple

@Simple(
    12 / 6,
    12L % 5L,
    12,

    3.3 - 3.0,
    3.3F * 2.0F,

    'a',

    true && false,
    false || true,
)
class WithSimpleOperations

@Simple(
    12.toByte()
)
class WithConversionCall

@ConversionCallConsumer(
    12.toByte()
)
class WithResolvedConversionCall

annotation class ConversionCallConsumer(val b: Byte)

@StringLiteral("some", "", "H$CONSTANT")
class WithStringLiteral

@StringLiteral("some" + "1", "" + CONSTANT + "2", "$CONSTANT" + "3")
class WithStringLiteralConcat

@StringLiteral($"$CONSTANT", $$"$$CONSTANT", $$$"$$$CONSTANT")
class WithStringInterpolationPrefix

@InvalidArguments("${CONSTANT ${}}")
class LongStringTemplateEntryWithTwoExpressions

@Deprecated("Deprecated", ReplaceWith("NewClass", "foo.bar.baz.NewClass"), DeprecationLevel.HIDDEN)
class Another

@Arrays([bar?.foo("str"), baz.bar?.doo, 1 != 2])
class WithIncorrectArguments

@kotlin.Deprecated("Deprecated", kotlin.ReplaceWith("NewClass", "foo.bar.baz.NewClass"), level = kotlin.DeprecationLevel.HIDDEN)
class Qualified

annotation class Arrays(
    val ia: IntArray,
    val la: LongArray,
    val fa: FloatArray,
    val da: DoubleArray,
    val ca: CharArray,
    val ba: BooleanArray,
)

annotation class ClassLiteral(val c1: KClass<*>, val c2: KClass<*>)

enum class E {
    E1, E2
}

annotation class EnumLiteral(val e1: E, val e2: E, val e3: E)

annotation class VarArg(vararg val v: Int) {
    companion object {
        const val CONSTANT = 3
    }
}

annotation class Nested(val i: Int, val s: String)

annotation class Outer(val some: String, val nested: Nested)

// All the parameters have default values as the annotation is used both with a single argument and with the full argument list
annotation class Simple(
    val i: Int = 0,
    val l: Long = 0,
    val b: Byte = 0,
    val d: Double = 0.0,
    val f: Float = 0.0F,
    val c: Char = ' ',
    val b1: Boolean = false,
    val b2: Boolean = false,
) {
    companion object {
        const val CONST1 = 1
        const val CONST2 = 2
    }
}

annotation class StringLiteral(val s1: String, val s2: String, val s3: String)

annotation class InvalidArguments(val s: String)

const val CONSTANT = 0
