// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun emptyInterpolation() {
    "padding ${} padding"
    $"padding ${} padding"
    $$"padding $${} padding"
    $$$$"padding $$$${} padding"
    $$$$$$$$"padding $$$$$$$${} padding"

    """padding ${} padding"""
    $"""padding ${} padding"""
    $$"""padding $${} padding"""
    $$$$"""padding $$$${} padding"""
    $$$$$$$$"""padding $$$$$$$${} padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun blankInterpolation() {
    "padding ${    } padding"
    $"padding ${    } padding"
    $$"padding $${    } padding"
    $$$$"padding $$$${    } padding"
    $$$$$$$$"padding $$$$$$$${    } padding"

    """padding ${    } padding"""
    $"""padding ${    } padding"""
    $$"""padding $${    } padding"""
    $$$$"""padding $$$${    } padding"""
    $$$$$$$$"""padding $$$$$$$${    } padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun linebreakInterpolation() {
    "padding ${
    } padding"
    $"padding ${
    } padding"
    $$"padding $${
    } padding"
    $$$$"padding $$$${
    } padding"
    $$$$$$$$"padding $$$$$$$${
    } padding"

    """padding ${
    } padding"""
    $"""padding ${
    } padding"""
    $$"""padding $${
    } padding"""
    $$$$"""padding $$$${
    } padding"""
    $$$$$$$$"""padding $$$$$$$${
    } padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfUnresolvedReference() {
    "padding $unresolved padding"
    $"padding $unresolved padding"
    $$"padding $$unresolved padding"
    $$$$"padding $$$$unresolved padding"
    $$$$$$$$"padding $$$$$$$$unresolved padding"

    "padding $`unresolved` padding"
    $"padding $`unresolved` padding"
    $$"padding $$`unresolved` padding"
    $$$$"padding $$$$`unresolved` padding"
    $$$$$$$$"padding $$$$$$$$`unresolved` padding"

    "padding ${unresolved} padding"
    $"padding ${unresolved} padding"
    $$"padding $${unresolved} padding"
    $$$$"padding $$$${unresolved} padding"
    $$$$$$$$"padding $$$$$$$${unresolved} padding"


    """padding $unresolved padding"""
    $"""padding $unresolved padding"""
    $$"""padding $$unresolved padding"""
    $$$$"""padding $$$$unresolved padding"""
    $$$$$$$$"""padding $$$$$$$$unresolved padding"""

    """padding $`unresolved` padding"""
    $"""padding $`unresolved` padding"""
    $$"""padding $$`unresolved` padding"""
    $$$$"""padding $$$$`unresolved` padding"""
    $$$$$$$$"""padding $$$$$$$$`unresolved` padding"""

    """padding ${unresolved} padding"""
    $"""padding ${unresolved} padding"""
    $$"""padding $${unresolved} padding"""
    $$$$"""padding $$$${unresolved} padding"""
    $$$$$$$$"""padding $$$$$$$${unresolved} padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfMisplacedDollar() {
    "padding $`$` padding"
    $"padding $`$` padding"
    $$"padding $$`$` padding"
    $$$$"padding $$$$`$` padding"
    $$$$$$$$"padding $$$$$$$$`$` padding"

    "padding ${$} padding"
    $"padding ${$} padding"
    $$"padding $${$} padding"
    $$$$"padding $$$${$} padding"
    $$$$$$$$"padding $$$$$$$${$} padding"


    """padding $`$` padding"""
    $"""padding $`$` padding"""
    $$"""padding $$`$` padding"""
    $$$$"""padding $$$$`$` padding"""
    $$$$$$$$"""padding $$$$$$$$`$` padding"""

    """padding ${$} padding"""
    $"""padding ${$} padding"""
    $$"""padding $${$} padding"""
    $$$$"""padding $$$${$} padding"""
    $$$$$$$$"""padding $$$$$$$${$} padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfMisplacedInterpolation() {
    "padding $`$value` padding"
    $"padding $`$value` padding"
    $$"padding $$`$$value` padding"
    $$$$"padding $$$$`$$$$value` padding"
    $$$$$$$$"padding $$$$$$$$`$$$$$$$$value` padding"

    "padding ${$value} padding"
    $"padding ${$value} padding"
    $$"padding $${$$value} padding"
    $$$$"padding $$$${$$$$value} padding"
    $$$$$$$$"padding $$$$$$$${$$$$$$$$value} padding"


    """padding $`$value` padding"""
    $"""padding $`$value` padding"""
    $$"""padding $$`$$value` padding"""
    $$$$"""padding $$$$`$$$$value` padding"""
    $$$$$$$$"""padding $$$$$$$$`$$$$$$$$value` padding"""

    """padding ${$value} padding"""
    $"""padding ${$value} padding"""
    $$"""padding $${$$value} padding"""
    $$$$"""padding $$$${$$$$value} padding"""
    $$$$$$$$"""padding $$$$$$$${$$$$$$$$value} padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfIncorrectExpression() {
    "padding ${42 +} padding"
    $"padding ${42 +} padding"
    $$"padding $${42 +} padding"
    $$$$"padding $$$${42 +} padding"
    $$$$$$$$"padding $$$$$$$${42 +} padding"

    """padding ${42 +} padding"""
    $"""padding ${42 +} padding"""
    $$"""padding $${42 +} padding"""
    $$$$"""padding $$$${42 +} padding"""
    $$$$$$$$"""padding $$$$$$$${42 +} padding"""
}

val runTimeConstant get() = 42

@Repeatable annotation class Annotation(val value: String)

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line

@Annotation("padding $runTimeConstant padding")
@Annotation($"padding $runTimeConstant padding")
@Annotation($$"padding $$runTimeConstant padding")
@Annotation($$$$"padding $$$$runTimeConstant padding")
@Annotation($$$$$$$$"padding $$$$$$$$runTimeConstant padding")

@Annotation("padding $`runTimeConstant` padding")
@Annotation($"padding $`runTimeConstant` padding")
@Annotation($$"padding $$`runTimeConstant` padding")
@Annotation($$$$"padding $$$$`runTimeConstant` padding")
@Annotation($$$$$$$$"padding $$$$$$$$`runTimeConstant` padding")

@Annotation("padding ${0 + runTimeConstant} padding")
@Annotation($"padding ${0 + runTimeConstant} padding")
@Annotation($$"padding $${0 + runTimeConstant} padding")
@Annotation($$$$"padding $$$${0 + runTimeConstant} padding")
@Annotation($$$$$$$$"padding $$$$$$$${0 + runTimeConstant} padding")


@Annotation("""padding $runTimeConstant padding""")
@Annotation($"""padding $runTimeConstant padding""")
@Annotation($$"""padding $$runTimeConstant padding""")
@Annotation($$$$"""padding $$$$runTimeConstant padding""")
@Annotation($$$$$$$$"""padding $$$$$$$$runTimeConstant padding""")

@Annotation("""padding $`runTimeConstant` padding""")
@Annotation($"""padding $`runTimeConstant` padding""")
@Annotation($$"""padding $$`runTimeConstant` padding""")
@Annotation($$$$"""padding $$$$`runTimeConstant` padding""")
@Annotation($$$$$$$$"""padding $$$$$$$$`runTimeConstant` padding""")

@Annotation("""padding ${0 + runTimeConstant} padding""")
@Annotation($"""padding ${0 + runTimeConstant} padding""")
@Annotation($$"""padding $${0 + runTimeConstant} padding""")
@Annotation($$$$"""padding $$$${0 + runTimeConstant} padding""")
@Annotation($$$$$$$$"""padding $$$$$$$${0 + runTimeConstant} padding""")

fun stringsWithInterpolationAsInvalidAnnotationArguments() {}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line

const val stringWithInterpolationAsInvalidConstantInitializer01 = "padding $runTimeConstant padding"
const val stringWithInterpolationAsInvalidConstantInitializer02 = $"padding $runTimeConstant padding"
const val stringWithInterpolationAsInvalidConstantInitializer03 = $$"padding $$runTimeConstant padding"
const val stringWithInterpolationAsInvalidConstantInitializer04 = $$$$"padding $$$$runTimeConstant padding"
const val stringWithInterpolationAsInvalidConstantInitializer05 = $$$$$$$$"padding $$$$$$$$runTimeConstant padding"

const val stringWithInterpolationAsInvalidConstantInitializer06 = "padding $`runTimeConstant` padding"
const val stringWithInterpolationAsInvalidConstantInitializer07 = $"padding $`runTimeConstant` padding"
const val stringWithInterpolationAsInvalidConstantInitializer08 = $$"padding $$`runTimeConstant` padding"
const val stringWithInterpolationAsInvalidConstantInitializer09 = $$$$"padding $$$$`runTimeConstant` padding"
const val stringWithInterpolationAsInvalidConstantInitializer10 = $$$$$$$$"padding $$$$$$$$`runTimeConstant` padding"

const val stringWithInterpolationAsInvalidConstantInitializer11 = "padding ${0 + runTimeConstant} padding"
const val stringWithInterpolationAsInvalidConstantInitializer12 = $"padding ${0 + runTimeConstant} padding"
const val stringWithInterpolationAsInvalidConstantInitializer13 = $$"padding $${0 + runTimeConstant} padding"
const val stringWithInterpolationAsInvalidConstantInitializer14 = $$$$"padding $$$${0 + runTimeConstant} padding"
const val stringWithInterpolationAsInvalidConstantInitializer15 = $$$$$$$$"padding $$$$$$$${0 + runTimeConstant} padding"


const val stringWithInterpolationAsInvalidConstantInitializer16 = """padding $runTimeConstant padding"""
const val stringWithInterpolationAsInvalidConstantInitializer17 = $"""padding $runTimeConstant padding"""
const val stringWithInterpolationAsInvalidConstantInitializer18 = $$"""padding $$runTimeConstant padding"""
const val stringWithInterpolationAsInvalidConstantInitializer19 = $$$$"""padding $$$$runTimeConstant padding"""
const val stringWithInterpolationAsInvalidConstantInitializer20 = $$$$$$$$"""padding $$$$$$$$runTimeConstant padding"""

const val stringWithInterpolationAsInvalidConstantInitializer21 = """padding $`runTimeConstant` padding"""
const val stringWithInterpolationAsInvalidConstantInitializer22 = $"""padding $`runTimeConstant` padding"""
const val stringWithInterpolationAsInvalidConstantInitializer23 = $$"""padding $$`runTimeConstant` padding"""
const val stringWithInterpolationAsInvalidConstantInitializer24 = $$$$"""padding $$$$`runTimeConstant` padding"""
const val stringWithInterpolationAsInvalidConstantInitializer25 = $$$$$$$$"""padding $$$$$$$$`runTimeConstant` padding"""

const val stringWithInterpolationAsInvalidConstantInitializer26 = """padding ${0 + runTimeConstant} padding"""
const val stringWithInterpolationAsInvalidConstantInitializer27 = $"""padding ${0 + runTimeConstant} padding"""
const val stringWithInterpolationAsInvalidConstantInitializer28 = $$"""padding $${0 + runTimeConstant} padding"""
const val stringWithInterpolationAsInvalidConstantInitializer29 = $$$$"""padding $$$${0 + runTimeConstant} padding"""
const val stringWithInterpolationAsInvalidConstantInitializer30 = $$$$$$$$"""padding $$$$$$$${0 + runTimeConstant} padding"""
