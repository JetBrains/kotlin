// LANGUAGE: +MultiDollarInterpolation
// WITH_EXTENDED_CHECKERS
// DIAGNOSTICS: -warnings +REDUNDANT_INTERPOLATION_PREFIX
// WITH_STDLIB

// COMPARE_WITH_LIGHT_TREE
// REASON: KT-68958

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun emptyInterpolation() {
    "<!SYNTAX{LT}!>${<!SYNTAX!><!>}<!>"
    <!REDUNDANT_INTERPOLATION_PREFIX{LT}!>$"<!SYNTAX{LT}!>${<!SYNTAX!><!>}<!>"<!>
    $$"<!SYNTAX{LT}!>$${<!SYNTAX!><!>}<!>"
    $$$$"<!SYNTAX{LT}!>$$$${<!SYNTAX!><!>}<!>"
    $$$$$$$$"<!SYNTAX{LT}!>$$$$$$$${<!SYNTAX!><!>}<!>"

    """<!SYNTAX{LT}!>${<!SYNTAX!><!>}<!>"""
    <!REDUNDANT_INTERPOLATION_PREFIX{LT}!>$"""<!SYNTAX{LT}!>${<!SYNTAX!><!>}<!>"""<!>
    $$"""<!SYNTAX{LT}!>$${<!SYNTAX!><!>}<!>"""
    $$$$"""<!SYNTAX{LT}!>$$$${<!SYNTAX!><!>}<!>"""
    $$$$$$$$"""<!SYNTAX{LT}!>$$$$$$$${<!SYNTAX!><!>}<!>"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun blankInterpolation() {
    "<!SYNTAX{LT}!>${<!SYNTAX!><!>    }<!>"
    <!REDUNDANT_INTERPOLATION_PREFIX{LT}!>$"<!SYNTAX{LT}!>${<!SYNTAX!><!>    }<!>"<!>
    $$"<!SYNTAX{LT}!>$${<!SYNTAX!><!>    }<!>"
    $$$$"<!SYNTAX{LT}!>$$$${<!SYNTAX!><!>    }<!>"
    $$$$$$$$"<!SYNTAX{LT}!>$$$$$$$${<!SYNTAX!><!>    }<!>"

    """<!SYNTAX{LT}!>${<!SYNTAX!><!>    }<!>"""
    <!REDUNDANT_INTERPOLATION_PREFIX{LT}!>$"""<!SYNTAX{LT}!>${<!SYNTAX!><!>    }<!>"""<!>
    $$"""<!SYNTAX{LT}!>$${<!SYNTAX!><!>    }<!>"""
    $$$$"""<!SYNTAX{LT}!>$$$${<!SYNTAX!><!>    }<!>"""
    $$$$$$$$"""<!SYNTAX{LT}!>$$$$$$$${<!SYNTAX!><!>    }<!>"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun linebreakInterpolation() {
    "<!SYNTAX{LT}!>${<!SYNTAX!><!>
    }<!>"
    <!REDUNDANT_INTERPOLATION_PREFIX{LT}!>$"<!SYNTAX{LT}!>${<!SYNTAX!><!>
    }<!>"<!>
    $$"<!SYNTAX{LT}!>$${<!SYNTAX!><!>
    }<!>"
    $$$$"<!SYNTAX{LT}!>$$$${<!SYNTAX!><!>
    }<!>"
    $$$$$$$$"<!SYNTAX{LT}!>$$$$$$$${<!SYNTAX!><!>
    }<!>"

    """<!SYNTAX{LT}!>${<!SYNTAX!><!>
    }<!>"""
    <!REDUNDANT_INTERPOLATION_PREFIX{LT}!>$"""<!SYNTAX{LT}!>${<!SYNTAX!><!>
    }<!>"""<!>
    $$"""<!SYNTAX{LT}!>$${<!SYNTAX!><!>
    }<!>"""
    $$$$"""<!SYNTAX{LT}!>$$$${<!SYNTAX!><!>
    }<!>"""
    $$$$$$$$"""<!SYNTAX{LT}!>$$$$$$$${<!SYNTAX!><!>
    }<!>"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfUnresolvedReference() {
    "$<!UNRESOLVED_REFERENCE!>unresolved<!>"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"$<!UNRESOLVED_REFERENCE!>unresolved<!>"<!>
    $$"$$<!UNRESOLVED_REFERENCE!>unresolved<!>"
    $$$$"$$$$<!UNRESOLVED_REFERENCE!>unresolved<!>"
    $$$$$$$$"$$$$$$$$<!UNRESOLVED_REFERENCE!>unresolved<!>"

    "$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"<!>
    $$"$$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"
    $$$$"$$$$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"
    $$$$$$$$"$$$$$$$$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"

    "${<!UNRESOLVED_REFERENCE!>unresolved<!>}"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"${<!UNRESOLVED_REFERENCE!>unresolved<!>}"<!>
    $$"$${<!UNRESOLVED_REFERENCE!>unresolved<!>}"
    $$$$"$$$${<!UNRESOLVED_REFERENCE!>unresolved<!>}"
    $$$$$$$$"$$$$$$$${<!UNRESOLVED_REFERENCE!>unresolved<!>}"


    """$<!UNRESOLVED_REFERENCE!>unresolved<!>"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""$<!UNRESOLVED_REFERENCE!>unresolved<!>"""<!>
    $$"""$$<!UNRESOLVED_REFERENCE!>unresolved<!>"""
    $$$$"""$$$$<!UNRESOLVED_REFERENCE!>unresolved<!>"""
    $$$$$$$$"""$$$$$$$$<!UNRESOLVED_REFERENCE!>unresolved<!>"""

    """$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"""<!>
    $$"""$$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"""
    $$$$"""$$$$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"""
    $$$$$$$$"""$$$$$$$$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"""

    """${<!UNRESOLVED_REFERENCE!>unresolved<!>}"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""${<!UNRESOLVED_REFERENCE!>unresolved<!>}"""<!>
    $$"""$${<!UNRESOLVED_REFERENCE!>unresolved<!>}"""
    $$$$"""$$$${<!UNRESOLVED_REFERENCE!>unresolved<!>}"""
    $$$$$$$$"""$$$$$$$${<!UNRESOLVED_REFERENCE!>unresolved<!>}"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfMisplacedDollar() {
    "$<!UNRESOLVED_REFERENCE!>`$`<!>"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"$<!UNRESOLVED_REFERENCE!>`$`<!>"<!>
    $$"$$<!UNRESOLVED_REFERENCE!>`$`<!>"
    $$$$"$$$$<!UNRESOLVED_REFERENCE!>`$`<!>"
    $$$$$$$$"$$$$$$$$<!UNRESOLVED_REFERENCE!>`$`<!>"

    "<!SYNTAX{LT}!>${<!SYNTAX!><!>$<!SYNTAX!><!>}<!>"
    <!REDUNDANT_INTERPOLATION_PREFIX{LT}!>$"<!SYNTAX{LT}!>${<!SYNTAX!><!>$<!SYNTAX!><!>}<!>"<!>
    $$"<!SYNTAX{LT}!>$${<!SYNTAX!><!>$<!SYNTAX!><!>}<!>"
    $$$$"<!SYNTAX{LT}!>$$$${<!SYNTAX!><!>$<!SYNTAX!><!>}<!>"
    $$$$$$$$"<!SYNTAX{LT}!>$$$$$$$${<!SYNTAX!><!>$<!SYNTAX!><!>}<!>"


    """$<!UNRESOLVED_REFERENCE!>`$`<!>"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""$<!UNRESOLVED_REFERENCE!>`$`<!>"""<!>
    $$"""$$<!UNRESOLVED_REFERENCE!>`$`<!>"""
    $$$$"""$$$$<!UNRESOLVED_REFERENCE!>`$`<!>"""
    $$$$$$$$"""$$$$$$$$<!UNRESOLVED_REFERENCE!>`$`<!>"""

    """<!SYNTAX{LT}!>${<!SYNTAX!><!>$<!SYNTAX!><!>}<!>"""
    <!REDUNDANT_INTERPOLATION_PREFIX{LT}!>$"""<!SYNTAX{LT}!>${<!SYNTAX!><!>$<!SYNTAX!><!>}<!>"""<!>
    $$"""<!SYNTAX{LT}!>$${<!SYNTAX!><!>$<!SYNTAX!><!>}<!>"""
    $$$$"""<!SYNTAX{LT}!>$$$${<!SYNTAX!><!>$<!SYNTAX!><!>}<!>"""
    $$$$$$$$"""<!SYNTAX{LT}!>$$$$$$$${<!SYNTAX!><!>$<!SYNTAX!><!>}<!>"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfMisplacedInterpolation() {
    "$<!UNRESOLVED_REFERENCE!>`$value`<!>"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"$<!UNRESOLVED_REFERENCE!>`$value`<!>"<!>
    $$"$$<!UNRESOLVED_REFERENCE!>`$$value`<!>"
    $$$$"$$$$<!UNRESOLVED_REFERENCE!>`$$$$value`<!>"
    $$$$$$$$"$$$$$$$$<!UNRESOLVED_REFERENCE!>`$$$$$$$$value`<!>"

    "${<!SYNTAX!><!>$value<!SYNTAX!><!>}"
    <!REDUNDANT_INTERPOLATION_PREFIX{LT}!>$"${<!SYNTAX!><!>$value<!SYNTAX!><!>}"<!>
    $$"$${<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>}"
    $$$$"$$$${<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>}"
    $$$$$$$$"$$$$$$$${<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>}"


    """$<!UNRESOLVED_REFERENCE!>`$value`<!>"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""$<!UNRESOLVED_REFERENCE!>`$value`<!>"""<!>
    $$"""$$<!UNRESOLVED_REFERENCE!>`$$value`<!>"""
    $$$$"""$$$$<!UNRESOLVED_REFERENCE!>`$$$$value`<!>"""
    $$$$$$$$"""$$$$$$$$<!UNRESOLVED_REFERENCE!>`$$$$$$$$value`<!>"""

    """${<!SYNTAX!><!>$value<!SYNTAX!><!>}"""
    <!REDUNDANT_INTERPOLATION_PREFIX{LT}!>$"""${<!SYNTAX!><!>$value<!SYNTAX!><!>}"""<!>
    $$"""$${<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>}"""
    $$$$"""$$$${<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>}"""
    $$$$$$$$"""$$$$$$$${<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>}"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfIncorrectExpression() {
    "${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>}"
    <!REDUNDANT_INTERPOLATION_PREFIX{LT}!>$"${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>}"<!>
    $$"$${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>}"
    $$$$"$$$${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>}"
    $$$$$$$$"$$$$$$$${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>}"

    """${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>}"""
    <!REDUNDANT_INTERPOLATION_PREFIX{LT}!>$"""${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>}"""<!>
    $$"""$${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>}"""
    $$$$"""$$$${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>}"""
    $$$$$$$$"""$$$$$$$${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>}"""
}

val runTimeConstant get() = 42

@Repeatable annotation class Annotation(val value: String)

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line

@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"$runTimeConstant"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, REDUNDANT_INTERPOLATION_PREFIX!>$"$runTimeConstant"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>$$"$$runTimeConstant"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>$$$$"$$$$runTimeConstant"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>$$$$$$$$"$$$$$$$$runTimeConstant"<!>)

@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"$`runTimeConstant`"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, REDUNDANT_INTERPOLATION_PREFIX!>$"$`runTimeConstant`"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>$$"$$`runTimeConstant`"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>$$$$"$$$$`runTimeConstant`"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>$$$$$$$$"$$$$$$$$`runTimeConstant`"<!>)

@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"${0 + runTimeConstant}"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, REDUNDANT_INTERPOLATION_PREFIX!>$"${0 + runTimeConstant}"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>$$"$${0 + runTimeConstant}"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>$$$$"$$$${0 + runTimeConstant}"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>$$$$$$$$"$$$$$$$${0 + runTimeConstant}"<!>)


@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"""$runTimeConstant"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, REDUNDANT_INTERPOLATION_PREFIX!>$"""$runTimeConstant"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>$$"""$$runTimeConstant"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>$$$$"""$$$$runTimeConstant"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>$$$$$$$$"""$$$$$$$$runTimeConstant"""<!>)

@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"""$`runTimeConstant`"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, REDUNDANT_INTERPOLATION_PREFIX!>$"""$`runTimeConstant`"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>$$"""$$`runTimeConstant`"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>$$$$"""$$$$`runTimeConstant`"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>$$$$$$$$"""$$$$$$$$`runTimeConstant`"""<!>)

@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"""${0 + runTimeConstant}"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, REDUNDANT_INTERPOLATION_PREFIX!>$"""${0 + runTimeConstant}"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>$$"""$${0 + runTimeConstant}"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>$$$$"""$$$${0 + runTimeConstant}"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>$$$$$$$$"""$$$$$$$${0 + runTimeConstant}"""<!>)

fun stringsWithInterpolationAsInvalidAnnotationArguments() {}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line

const val stringWithInterpolationAsInvalidConstantInitializer01 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"$runTimeConstant"<!>
const val stringWithInterpolationAsInvalidConstantInitializer02 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, REDUNDANT_INTERPOLATION_PREFIX!>$"$runTimeConstant"<!>
const val stringWithInterpolationAsInvalidConstantInitializer03 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>$$"$$runTimeConstant"<!>
const val stringWithInterpolationAsInvalidConstantInitializer04 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>$$$$"$$$$runTimeConstant"<!>
const val stringWithInterpolationAsInvalidConstantInitializer05 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>$$$$$$$$"$$$$$$$$runTimeConstant"<!>

const val stringWithInterpolationAsInvalidConstantInitializer06 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"$`runTimeConstant`"<!>
const val stringWithInterpolationAsInvalidConstantInitializer07 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, REDUNDANT_INTERPOLATION_PREFIX!>$"$`runTimeConstant`"<!>
const val stringWithInterpolationAsInvalidConstantInitializer08 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>$$"$$`runTimeConstant`"<!>
const val stringWithInterpolationAsInvalidConstantInitializer09 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>$$$$"$$$$`runTimeConstant`"<!>
const val stringWithInterpolationAsInvalidConstantInitializer10 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>$$$$$$$$"$$$$$$$$`runTimeConstant`"<!>

const val stringWithInterpolationAsInvalidConstantInitializer11 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"${0 + runTimeConstant}"<!>
const val stringWithInterpolationAsInvalidConstantInitializer12 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, REDUNDANT_INTERPOLATION_PREFIX!>$"${0 + runTimeConstant}"<!>
const val stringWithInterpolationAsInvalidConstantInitializer13 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>$$"$${0 + runTimeConstant}"<!>
const val stringWithInterpolationAsInvalidConstantInitializer14 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>$$$$"$$$${0 + runTimeConstant}"<!>
const val stringWithInterpolationAsInvalidConstantInitializer15 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>$$$$$$$$"$$$$$$$${0 + runTimeConstant}"<!>


const val stringWithInterpolationAsInvalidConstantInitializer16 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"""$runTimeConstant"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer17 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, REDUNDANT_INTERPOLATION_PREFIX!>$"""$runTimeConstant"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer18 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>$$"""$$runTimeConstant"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer19 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>$$$$"""$$$$runTimeConstant"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer20 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>$$$$$$$$"""$$$$$$$$runTimeConstant"""<!>

const val stringWithInterpolationAsInvalidConstantInitializer21 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"""$`runTimeConstant`"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer22 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, REDUNDANT_INTERPOLATION_PREFIX!>$"""$`runTimeConstant`"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer23 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>$$"""$$`runTimeConstant`"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer24 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>$$$$"""$$$$`runTimeConstant`"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer25 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>$$$$$$$$"""$$$$$$$$`runTimeConstant`"""<!>

const val stringWithInterpolationAsInvalidConstantInitializer26 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"""${0 + runTimeConstant}"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer27 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, REDUNDANT_INTERPOLATION_PREFIX!>$"""${0 + runTimeConstant}"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer28 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>$$"""$${0 + runTimeConstant}"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer29 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>$$$$"""$$$${0 + runTimeConstant}"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer30 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>$$$$$$$$"""$$$$$$$${0 + runTimeConstant}"""<!>

// interpolation prefix length: 1, 2, 4, 8
fun orphanedInterpolationPrefix() {
    <!SYNTAX!>$<!>
    <!SYNTAX!>$<!><!SYNTAX!>$<!>
    <!SYNTAX!>$<!><!SYNTAX!>$$$<!>
    <!SYNTAX!>$<!><!SYNTAX!>$$$$$$$<!>
}

// interpolation prefix length: 1, 2, 4, 8
// string literal kinds: single-line, multi-line
fun separatedInterpolationPrefix() {
    <!SYNTAX!>$<!> "padding"
    <!SYNTAX!>$<!><!SYNTAX!>$ "padding"<!>
    <!SYNTAX!>$<!><!SYNTAX!>$$$ "padding"<!>
    <!SYNTAX!>$<!><!SYNTAX!>$$$$$$$ "padding"<!>

    <!SYNTAX!>$<!> """padding"""
    <!SYNTAX!>$<!><!SYNTAX!>$ """padding"""<!>
    <!SYNTAX!>$<!><!SYNTAX!>$$$ """padding"""<!>
    <!SYNTAX!>$<!><!SYNTAX!>$$$$$$$ """padding"""<!>
}
