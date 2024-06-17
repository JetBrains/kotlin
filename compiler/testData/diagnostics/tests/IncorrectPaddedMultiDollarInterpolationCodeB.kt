// LANGUAGE: -MultiDollarInterpolation
// WITH_STDLIB

// COMPARE_WITH_LIGHT_TREE
// REASON: differences in syntax error reporting

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun emptyInterpolation() {
    "padding ${<!SYNTAX!><!>} padding"
    <!UNSUPPORTED_FEATURE!>$"padding ${<!SYNTAX!><!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!SYNTAX!><!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!SYNTAX!><!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!SYNTAX!><!>} padding"<!>

    """padding ${<!SYNTAX!><!>} padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!SYNTAX!><!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!SYNTAX!><!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!SYNTAX!><!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!SYNTAX!><!>} padding"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun blankInterpolation() {
    "padding ${<!SYNTAX!><!>    } padding"
    <!UNSUPPORTED_FEATURE!>$"padding ${<!SYNTAX!><!>    } padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!SYNTAX!><!>    } padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!SYNTAX!><!>    } padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!SYNTAX!><!>    } padding"<!>

    """padding ${<!SYNTAX!><!>    } padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!SYNTAX!><!>    } padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!SYNTAX!><!>    } padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!SYNTAX!><!>    } padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!SYNTAX!><!>    } padding"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun linebreakInterpolation() {
    "padding ${<!SYNTAX!><!>
    } padding"
    <!UNSUPPORTED_FEATURE!>$"padding ${<!SYNTAX!><!>
    } padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!SYNTAX!><!>
    } padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!SYNTAX!><!>
    } padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!SYNTAX!><!>
    } padding"<!>

    """padding ${<!SYNTAX!><!>
    } padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!SYNTAX!><!>
    } padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!SYNTAX!><!>
    } padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!SYNTAX!><!>
    } padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!SYNTAX!><!>
    } padding"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfUnresolvedReference() {
    "padding $<!UNRESOLVED_REFERENCE!>unresolved<!> padding"
    <!UNSUPPORTED_FEATURE!>$"padding $<!UNRESOLVED_REFERENCE!>unresolved<!> padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$<!UNRESOLVED_REFERENCE!>unresolved<!> padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$<!UNRESOLVED_REFERENCE!>unresolved<!> padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$<!UNRESOLVED_REFERENCE!>unresolved<!> padding"<!>

    "padding $<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"
    <!UNSUPPORTED_FEATURE!>$"padding $<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"<!>

    "padding ${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"<!>


    """padding $<!UNRESOLVED_REFERENCE!>unresolved<!> padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $<!UNRESOLVED_REFERENCE!>unresolved<!> padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$<!UNRESOLVED_REFERENCE!>unresolved<!> padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$<!UNRESOLVED_REFERENCE!>unresolved<!> padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$<!UNRESOLVED_REFERENCE!>unresolved<!> padding"""<!>

    """padding $<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"""<!>

    """padding ${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfMisplacedDollar() {
    "padding $<!UNRESOLVED_REFERENCE!>`$`<!> padding"
    <!UNSUPPORTED_FEATURE!>$"padding $<!UNRESOLVED_REFERENCE!>`$`<!> padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$<!UNRESOLVED_REFERENCE!>`$`<!> padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$<!UNRESOLVED_REFERENCE!>`$`<!> padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$<!UNRESOLVED_REFERENCE!>`$`<!> padding"<!>

    "padding ${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"
    <!UNSUPPORTED_FEATURE!>$"padding ${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"<!>


    """padding $<!UNRESOLVED_REFERENCE!>`$`<!> padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $<!UNRESOLVED_REFERENCE!>`$`<!> padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$<!UNRESOLVED_REFERENCE!>`$`<!> padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$<!UNRESOLVED_REFERENCE!>`$`<!> padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$<!UNRESOLVED_REFERENCE!>`$`<!> padding"""<!>

    """padding ${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfMisplacedInterpolation() {
    "padding $<!UNRESOLVED_REFERENCE!>`$value`<!> padding"
    <!UNSUPPORTED_FEATURE!>$"padding $<!UNRESOLVED_REFERENCE!>`$value`<!> padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$<!UNRESOLVED_REFERENCE!>`$$value`<!> padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$<!UNRESOLVED_REFERENCE!>`$$$$value`<!> padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$<!UNRESOLVED_REFERENCE!>`$$$$$$$$value`<!> padding"<!>

    "padding ${<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"
    <!UNSUPPORTED_FEATURE!>$"padding ${<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"<!>


    """padding $<!UNRESOLVED_REFERENCE!>`$value`<!> padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $<!UNRESOLVED_REFERENCE!>`$value`<!> padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$<!UNRESOLVED_REFERENCE!>`$$value`<!> padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$<!UNRESOLVED_REFERENCE!>`$$$$value`<!> padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$<!UNRESOLVED_REFERENCE!>`$$$$$$$$value`<!> padding"""<!>

    """padding ${<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfIncorrectExpression() {
    "padding ${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>} padding"
    <!UNSUPPORTED_FEATURE!>$"padding ${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>} padding"<!>

    """padding ${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>} padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding ${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${42 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>} padding"""<!>
}

val runTimeConstant get() = 42

@Repeatable annotation class Annotation(val value: String)

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line

@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"padding $runTimeConstant padding"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$"padding $runTimeConstant padding"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$$"padding $$runTimeConstant padding"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$$$$"padding $$$$runTimeConstant padding"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$runTimeConstant padding"<!>)

@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"padding $`runTimeConstant` padding"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$"padding $`runTimeConstant` padding"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$$"padding $$`runTimeConstant` padding"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$$$$"padding $$$$`runTimeConstant` padding"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$`runTimeConstant` padding"<!>)

@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"padding ${0 + runTimeConstant} padding"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$"padding ${0 + runTimeConstant} padding"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$$"padding $${0 + runTimeConstant} padding"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$$$$"padding $$$${0 + runTimeConstant} padding"<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${0 + runTimeConstant} padding"<!>)


@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"""padding $runTimeConstant padding"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$"""padding $runTimeConstant padding"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$$"""padding $$runTimeConstant padding"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$$$$"""padding $$$$runTimeConstant padding"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$runTimeConstant padding"""<!>)

@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"""padding $`runTimeConstant` padding"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$"""padding $`runTimeConstant` padding"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$$"""padding $$`runTimeConstant` padding"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$$$$"""padding $$$$`runTimeConstant` padding"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$`runTimeConstant` padding"""<!>)

@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"""padding ${0 + runTimeConstant} padding"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$"""padding ${0 + runTimeConstant} padding"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$$"""padding $${0 + runTimeConstant} padding"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$$$$"""padding $$$${0 + runTimeConstant} padding"""<!>)
@Annotation(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${0 + runTimeConstant} padding"""<!>)

fun stringsWithInterpolationAsInvalidAnnotationArguments() {}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line

const val stringWithInterpolationAsInvalidConstantInitializer01 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"padding $runTimeConstant padding"<!>
const val stringWithInterpolationAsInvalidConstantInitializer02 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$"padding $runTimeConstant padding"<!>
const val stringWithInterpolationAsInvalidConstantInitializer03 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$$"padding $$runTimeConstant padding"<!>
const val stringWithInterpolationAsInvalidConstantInitializer04 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$$$$"padding $$$$runTimeConstant padding"<!>
const val stringWithInterpolationAsInvalidConstantInitializer05 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$runTimeConstant padding"<!>

const val stringWithInterpolationAsInvalidConstantInitializer06 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"padding $`runTimeConstant` padding"<!>
const val stringWithInterpolationAsInvalidConstantInitializer07 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$"padding $`runTimeConstant` padding"<!>
const val stringWithInterpolationAsInvalidConstantInitializer08 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$$"padding $$`runTimeConstant` padding"<!>
const val stringWithInterpolationAsInvalidConstantInitializer09 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$$$$"padding $$$$`runTimeConstant` padding"<!>
const val stringWithInterpolationAsInvalidConstantInitializer10 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$`runTimeConstant` padding"<!>

const val stringWithInterpolationAsInvalidConstantInitializer11 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"padding ${0 + runTimeConstant} padding"<!>
const val stringWithInterpolationAsInvalidConstantInitializer12 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$"padding ${0 + runTimeConstant} padding"<!>
const val stringWithInterpolationAsInvalidConstantInitializer13 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$$"padding $${0 + runTimeConstant} padding"<!>
const val stringWithInterpolationAsInvalidConstantInitializer14 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$$$$"padding $$$${0 + runTimeConstant} padding"<!>
const val stringWithInterpolationAsInvalidConstantInitializer15 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${0 + runTimeConstant} padding"<!>


const val stringWithInterpolationAsInvalidConstantInitializer16 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"""padding $runTimeConstant padding"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer17 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$"""padding $runTimeConstant padding"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer18 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$$"""padding $$runTimeConstant padding"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer19 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$$$$"""padding $$$$runTimeConstant padding"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer20 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$runTimeConstant padding"""<!>

const val stringWithInterpolationAsInvalidConstantInitializer21 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"""padding $`runTimeConstant` padding"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer22 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$"""padding $`runTimeConstant` padding"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer23 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$$"""padding $$`runTimeConstant` padding"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer24 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$$$$"""padding $$$$`runTimeConstant` padding"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer25 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$`runTimeConstant` padding"""<!>

const val stringWithInterpolationAsInvalidConstantInitializer26 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"""padding ${0 + runTimeConstant} padding"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer27 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$"""padding ${0 + runTimeConstant} padding"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer28 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$$"""padding $${0 + runTimeConstant} padding"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer29 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$$$$"""padding $$$${0 + runTimeConstant} padding"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer30 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${0 + runTimeConstant} padding"""<!>
