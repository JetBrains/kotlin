// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun emptyInterpolation() {
    "${}"
    $"${}"
    $$"$${}"
    $$$$"$$$${}"
    $$$$$$$$"$$$$$$$${}"

    """${}"""
    $"""${}"""
    $$"""$${}"""
    $$$$"""$$$${}"""
    $$$$$$$$"""$$$$$$$${}"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun blankInterpolation() {
    "${    }"
    $"${    }"
    $$"$${    }"
    $$$$"$$$${    }"
    $$$$$$$$"$$$$$$$${    }"

    """${    }"""
    $"""${    }"""
    $$"""$${    }"""
    $$$$"""$$$${    }"""
    $$$$$$$$"""$$$$$$$${    }"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun linebreakInterpolation() {
    "${
    }"
    $"${
    }"
    $$"$${
    }"
    $$$$"$$$${
    }"
    $$$$$$$$"$$$$$$$${
    }"

    """${
    }"""
    $"""${
    }"""
    $$"""$${
    }"""
    $$$$"""$$$${
    }"""
    $$$$$$$$"""$$$$$$$${
    }"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfUnresolvedReference() {
    "$unresolved"
    $"$unresolved"
    $$"$$unresolved"
    $$$$"$$$$unresolved"
    $$$$$$$$"$$$$$$$$unresolved"

    "$`unresolved`"
    $"$`unresolved`"
    $$"$$`unresolved`"
    $$$$"$$$$`unresolved`"
    $$$$$$$$"$$$$$$$$`unresolved`"

    "${unresolved}"
    $"${unresolved}"
    $$"$${unresolved}"
    $$$$"$$$${unresolved}"
    $$$$$$$$"$$$$$$$${unresolved}"


    """$unresolved"""
    $"""$unresolved"""
    $$"""$$unresolved"""
    $$$$"""$$$$unresolved"""
    $$$$$$$$"""$$$$$$$$unresolved"""

    """$`unresolved`"""
    $"""$`unresolved`"""
    $$"""$$`unresolved`"""
    $$$$"""$$$$`unresolved`"""
    $$$$$$$$"""$$$$$$$$`unresolved`"""

    """${unresolved}"""
    $"""${unresolved}"""
    $$"""$${unresolved}"""
    $$$$"""$$$${unresolved}"""
    $$$$$$$$"""$$$$$$$${unresolved}"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfMisplacedDollar() {
    "$`$`"
    $"$`$`"
    $$"$$`$`"
    $$$$"$$$$`$`"
    $$$$$$$$"$$$$$$$$`$`"

    "${$}"
    $"${$}"
    $$"$${$}"
    $$$$"$$$${$}"
    $$$$$$$$"$$$$$$$${$}"


    """$`$`"""
    $"""$`$`"""
    $$"""$$`$`"""
    $$$$"""$$$$`$`"""
    $$$$$$$$"""$$$$$$$$`$`"""

    """${$}"""
    $"""${$}"""
    $$"""$${$}"""
    $$$$"""$$$${$}"""
    $$$$$$$$"""$$$$$$$${$}"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfMisplacedInterpolation() {
    "$`$value`"
    $"$`$value`"
    $$"$$`$$value`"
    $$$$"$$$$`$$$$value`"
    $$$$$$$$"$$$$$$$$`$$$$$$$$value`"

    "${$value}"
    $"${$value}"
    $$"$${$$value}"
    $$$$"$$$${$$$$value}"
    $$$$$$$$"$$$$$$$${$$$$$$$$value}"


    """$`$value`"""
    $"""$`$value`"""
    $$"""$$`$$value`"""
    $$$$"""$$$$`$$$$value`"""
    $$$$$$$$"""$$$$$$$$`$$$$$$$$value`"""

    """${$value}"""
    $"""${$value}"""
    $$"""$${$$value}"""
    $$$$"""$$$${$$$$value}"""
    $$$$$$$$"""$$$$$$$${$$$$$$$$value}"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfIncorrectExpression() {
    "${42 +}"
    $"${42 +}"
    $$"$${42 +}"
    $$$$"$$$${42 +}"
    $$$$$$$$"$$$$$$$${42 +}"

    """${42 +}"""
    $"""${42 +}"""
    $$"""$${42 +}"""
    $$$$"""$$$${42 +}"""
    $$$$$$$$"""$$$$$$$${42 +}"""
}

val runTimeConstant get() = 42

@Repeatable annotation class Annotation(val value: String)

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line

@Annotation("$runTimeConstant")
@Annotation($"$runTimeConstant")
@Annotation($$"$$runTimeConstant")
@Annotation($$$$"$$$$runTimeConstant")
@Annotation($$$$$$$$"$$$$$$$$runTimeConstant")

@Annotation("$`runTimeConstant`")
@Annotation($"$`runTimeConstant`")
@Annotation($$"$$`runTimeConstant`")
@Annotation($$$$"$$$$`runTimeConstant`")
@Annotation($$$$$$$$"$$$$$$$$`runTimeConstant`")

@Annotation("${0 + runTimeConstant}")
@Annotation($"${0 + runTimeConstant}")
@Annotation($$"$${0 + runTimeConstant}")
@Annotation($$$$"$$$${0 + runTimeConstant}")
@Annotation($$$$$$$$"$$$$$$$${0 + runTimeConstant}")


@Annotation("""$runTimeConstant""")
@Annotation($"""$runTimeConstant""")
@Annotation($$"""$$runTimeConstant""")
@Annotation($$$$"""$$$$runTimeConstant""")
@Annotation($$$$$$$$"""$$$$$$$$runTimeConstant""")

@Annotation("""$`runTimeConstant`""")
@Annotation($"""$`runTimeConstant`""")
@Annotation($$"""$$`runTimeConstant`""")
@Annotation($$$$"""$$$$`runTimeConstant`""")
@Annotation($$$$$$$$"""$$$$$$$$`runTimeConstant`""")

@Annotation("""${0 + runTimeConstant}""")
@Annotation($"""${0 + runTimeConstant}""")
@Annotation($$"""$${0 + runTimeConstant}""")
@Annotation($$$$"""$$$${0 + runTimeConstant}""")
@Annotation($$$$$$$$"""$$$$$$$${0 + runTimeConstant}""")

fun stringsWithInterpolationAsInvalidAnnotationArguments() {}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line

const val stringWithInterpolationAsInvalidConstantInitializer01 = "$runTimeConstant"
const val stringWithInterpolationAsInvalidConstantInitializer02 = $"$runTimeConstant"
const val stringWithInterpolationAsInvalidConstantInitializer03 = $$"$$runTimeConstant"
const val stringWithInterpolationAsInvalidConstantInitializer04 = $$$$"$$$$runTimeConstant"
const val stringWithInterpolationAsInvalidConstantInitializer05 = $$$$$$$$"$$$$$$$$runTimeConstant"

const val stringWithInterpolationAsInvalidConstantInitializer06 = "$`runTimeConstant`"
const val stringWithInterpolationAsInvalidConstantInitializer07 = $"$`runTimeConstant`"
const val stringWithInterpolationAsInvalidConstantInitializer08 = $$"$$`runTimeConstant`"
const val stringWithInterpolationAsInvalidConstantInitializer09 = $$$$"$$$$`runTimeConstant`"
const val stringWithInterpolationAsInvalidConstantInitializer10 = $$$$$$$$"$$$$$$$$`runTimeConstant`"

const val stringWithInterpolationAsInvalidConstantInitializer11 = "${0 + runTimeConstant}"
const val stringWithInterpolationAsInvalidConstantInitializer12 = $"${0 + runTimeConstant}"
const val stringWithInterpolationAsInvalidConstantInitializer13 = $$"$${0 + runTimeConstant}"
const val stringWithInterpolationAsInvalidConstantInitializer14 = $$$$"$$$${0 + runTimeConstant}"
const val stringWithInterpolationAsInvalidConstantInitializer15 = $$$$$$$$"$$$$$$$${0 + runTimeConstant}"


const val stringWithInterpolationAsInvalidConstantInitializer16 = """$runTimeConstant"""
const val stringWithInterpolationAsInvalidConstantInitializer17 = $"""$runTimeConstant"""
const val stringWithInterpolationAsInvalidConstantInitializer18 = $$"""$$runTimeConstant"""
const val stringWithInterpolationAsInvalidConstantInitializer19 = $$$$"""$$$$runTimeConstant"""
const val stringWithInterpolationAsInvalidConstantInitializer20 = $$$$$$$$"""$$$$$$$$runTimeConstant"""

const val stringWithInterpolationAsInvalidConstantInitializer21 = """$`runTimeConstant`"""
const val stringWithInterpolationAsInvalidConstantInitializer22 = $"""$`runTimeConstant`"""
const val stringWithInterpolationAsInvalidConstantInitializer23 = $$"""$$`runTimeConstant`"""
const val stringWithInterpolationAsInvalidConstantInitializer24 = $$$$"""$$$$`runTimeConstant`"""
const val stringWithInterpolationAsInvalidConstantInitializer25 = $$$$$$$$"""$$$$$$$$`runTimeConstant`"""

const val stringWithInterpolationAsInvalidConstantInitializer26 = """${0 + runTimeConstant}"""
const val stringWithInterpolationAsInvalidConstantInitializer27 = $"""${0 + runTimeConstant}"""
const val stringWithInterpolationAsInvalidConstantInitializer28 = $$"""$${0 + runTimeConstant}"""
const val stringWithInterpolationAsInvalidConstantInitializer29 = $$$$"""$$$${0 + runTimeConstant}"""
const val stringWithInterpolationAsInvalidConstantInitializer30 = $$$$$$$$"""$$$$$$$${0 + runTimeConstant}"""

// interpolation prefix length: 1, 2, 4, 8
fun orphanedInterpolationPrefix() {
    $
    $$
    $$$$
    $$$$$$$$
}

// interpolation prefix length: 1, 2, 4, 8
// string literal kinds: single-line, multi-line
fun separatedInterpolationPrefix() {
    $ "padding"
    $$ "padding"
    $$$$ "padding"
    $$$$$$$$ "padding"

    $ """padding"""
    $$ """padding"""
    $$$$ """padding"""
    $$$$$$$$ """padding"""
}
