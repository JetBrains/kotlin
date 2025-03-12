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

@Outer("value", foo.Nested(1, "nested value"))
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

@StringLiteral("some", "", "H$CONSTANT")
class WithStringLiteral

@StringLiteral("some" + "1", "" + CONSTANT + "2", "$CONSTANT" + "3")
class WithStringLiteralConcat

@StringLiteral($"$CONSTANT", $$"$$CONSTANT", $$$"$$$CONSTANT")
class WithStringInterpolationPrefix
