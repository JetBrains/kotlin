// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiDollarInterpolation
// WITH_EXPERIMENTAL_CHECKERS
// DIAGNOSTICS: -warnings +REDUNDANT_INTERPOLATION_PREFIX
// WITH_STDLIB

// COMPARE_WITH_LIGHT_TREE
// REASON: differences in syntax error reporting

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun emptyInterpolation() {
    "padding ${<!SYNTAX!><!>} padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${<!SYNTAX!><!>} padding"<!>
    $$"padding $${<!SYNTAX!><!>} padding"
    $$$$"padding $$$${<!SYNTAX!><!>} padding"
    $$$$$$$$"padding $$$$$$$${<!SYNTAX!><!>} padding"

    """padding ${<!SYNTAX!><!>} padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${<!SYNTAX!><!>} padding"""<!>
    $$"""padding $${<!SYNTAX!><!>} padding"""
    $$$$"""padding $$$${<!SYNTAX!><!>} padding"""
    $$$$$$$$"""padding $$$$$$$${<!SYNTAX!><!>} padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun blankInterpolation() {
    "padding ${<!SYNTAX!><!>    } padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${<!SYNTAX!><!>    } padding"<!>
    $$"padding $${<!SYNTAX!><!>    } padding"
    $$$$"padding $$$${<!SYNTAX!><!>    } padding"
    $$$$$$$$"padding $$$$$$$${<!SYNTAX!><!>    } padding"

    """padding ${<!SYNTAX!><!>    } padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${<!SYNTAX!><!>    } padding"""<!>
    $$"""padding $${<!SYNTAX!><!>    } padding"""
    $$$$"""padding $$$${<!SYNTAX!><!>    } padding"""
    $$$$$$$$"""padding $$$$$$$${<!SYNTAX!><!>    } padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun linebreakInterpolation() {
    "padding ${<!SYNTAX!><!>
    } padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${<!SYNTAX!><!>
    } padding"<!>
    $$"padding $${<!SYNTAX!><!>
    } padding"
    $$$$"padding $$$${<!SYNTAX!><!>
    } padding"
    $$$$$$$$"padding $$$$$$$${<!SYNTAX!><!>
    } padding"

    """padding ${<!SYNTAX!><!>
    } padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${<!SYNTAX!><!>
    } padding"""<!>
    $$"""padding $${<!SYNTAX!><!>
    } padding"""
    $$$$"""padding $$$${<!SYNTAX!><!>
    } padding"""
    $$$$$$$$"""padding $$$$$$$${<!SYNTAX!><!>
    } padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfUnresolvedReference() {
    "padding $<!UNRESOLVED_REFERENCE!>unresolved<!> padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $<!UNRESOLVED_REFERENCE!>unresolved<!> padding"<!>
    $$"padding $$<!UNRESOLVED_REFERENCE!>unresolved<!> padding"
    $$$$"padding $$$$<!UNRESOLVED_REFERENCE!>unresolved<!> padding"
    $$$$$$$$"padding $$$$$$$$<!UNRESOLVED_REFERENCE!>unresolved<!> padding"

    "padding $<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"<!>
    $$"padding $$<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"
    $$$$"padding $$$$<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"
    $$$$$$$$"padding $$$$$$$$<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"

    "padding ${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"<!>
    $$"padding $${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"
    $$$$"padding $$$${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"
    $$$$$$$$"padding $$$$$$$${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"


    """padding $<!UNRESOLVED_REFERENCE!>unresolved<!> padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $<!UNRESOLVED_REFERENCE!>unresolved<!> padding"""<!>
    $$"""padding $$<!UNRESOLVED_REFERENCE!>unresolved<!> padding"""
    $$$$"""padding $$$$<!UNRESOLVED_REFERENCE!>unresolved<!> padding"""
    $$$$$$$$"""padding $$$$$$$$<!UNRESOLVED_REFERENCE!>unresolved<!> padding"""

    """padding $<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"""<!>
    $$"""padding $$<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"""
    $$$$"""padding $$$$<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"""
    $$$$$$$$"""padding $$$$$$$$<!UNRESOLVED_REFERENCE!>`unresolved`<!> padding"""

    """padding ${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"""<!>
    $$"""padding $${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"""
    $$$$"""padding $$$${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"""
    $$$$$$$$"""padding $$$$$$$${<!UNRESOLVED_REFERENCE!>unresolved<!>} padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfMisplacedDollar() {
    "padding $<!UNRESOLVED_REFERENCE!>`$`<!> padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $<!UNRESOLVED_REFERENCE!>`$`<!> padding"<!>
    $$"padding $$<!UNRESOLVED_REFERENCE!>`$`<!> padding"
    $$$$"padding $$$$<!UNRESOLVED_REFERENCE!>`$`<!> padding"
    $$$$$$$$"padding $$$$$$$$<!UNRESOLVED_REFERENCE!>`$`<!> padding"

    "padding ${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"<!>
    $$"padding $${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"
    $$$$"padding $$$${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"
    $$$$$$$$"padding $$$$$$$${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"


    """padding $<!UNRESOLVED_REFERENCE!>`$`<!> padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $<!UNRESOLVED_REFERENCE!>`$`<!> padding"""<!>
    $$"""padding $$<!UNRESOLVED_REFERENCE!>`$`<!> padding"""
    $$$$"""padding $$$$<!UNRESOLVED_REFERENCE!>`$`<!> padding"""
    $$$$$$$$"""padding $$$$$$$$<!UNRESOLVED_REFERENCE!>`$`<!> padding"""

    """padding ${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"""<!>
    $$"""padding $${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"""
    $$$$"""padding $$$${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"""
    $$$$$$$$"""padding $$$$$$$${<!SYNTAX!><!>$<!SYNTAX!><!>} padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfMisplacedInterpolation() {
    "padding $<!UNRESOLVED_REFERENCE!>`$value`<!> padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $<!UNRESOLVED_REFERENCE!>`$value`<!> padding"<!>
    $$"padding $$<!UNRESOLVED_REFERENCE!>`$$value`<!> padding"
    $$$$"padding $$$$<!UNRESOLVED_REFERENCE!>`$$$$value`<!> padding"
    $$$$$$$$"padding $$$$$$$$<!UNRESOLVED_REFERENCE!>`$$$$$$$$value`<!> padding"

    "padding ${<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"<!>
    $$"padding $${<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"
    $$$$"padding $$$${<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"
    $$$$$$$$"padding $$$$$$$${<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"


    """padding $<!UNRESOLVED_REFERENCE!>`$value`<!> padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $<!UNRESOLVED_REFERENCE!>`$value`<!> padding"""<!>
    $$"""padding $$<!UNRESOLVED_REFERENCE!>`$$value`<!> padding"""
    $$$$"""padding $$$$<!UNRESOLVED_REFERENCE!>`$$$$value`<!> padding"""
    $$$$$$$$"""padding $$$$$$$$<!UNRESOLVED_REFERENCE!>`$$$$$$$$value`<!> padding"""

    """padding ${<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"""<!>
    $$"""padding $${<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"""
    $$$$"""padding $$$${<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"""
    $$$$$$$$"""padding $$$$$$$${<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>} padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfIncorrectExpression() {
    "padding ${42 +<!SYNTAX!><!>} padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${42 +<!SYNTAX!><!>} padding"<!>
    $$"padding $${42 +<!SYNTAX!><!>} padding"
    $$$$"padding $$$${42 +<!SYNTAX!><!>} padding"
    $$$$$$$$"padding $$$$$$$${42 +<!SYNTAX!><!>} padding"

    """padding ${42 +<!SYNTAX!><!>} padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${42 +<!SYNTAX!><!>} padding"""<!>
    $$"""padding $${42 +<!SYNTAX!><!>} padding"""
    $$$$"""padding $$$${42 +<!SYNTAX!><!>} padding"""
    $$$$$$$$"""padding $$$$$$$${42 +<!SYNTAX!><!>} padding"""
}

val runTimeConstant get() = 42

@Repeatable annotation class Annotation(val value: String)

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line

@Annotation("padding $<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!> padding")
@Annotation(<!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!> padding"<!>)
@Annotation($$"padding $$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!> padding")
@Annotation($$$$"padding $$$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!> padding")
@Annotation($$$$$$$$"padding $$$$$$$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!> padding")

@Annotation("padding $<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!> padding")
@Annotation(<!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!> padding"<!>)
@Annotation($$"padding $$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!> padding")
@Annotation($$$$"padding $$$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!> padding")
@Annotation($$$$$$$$"padding $$$$$$$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!> padding")

@Annotation("padding ${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>} padding")
@Annotation(<!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>} padding"<!>)
@Annotation($$"padding $${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>} padding")
@Annotation($$$$"padding $$$${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>} padding")
@Annotation($$$$$$$$"padding $$$$$$$${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>} padding")


@Annotation("""padding $<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!> padding""")
@Annotation(<!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!> padding"""<!>)
@Annotation($$"""padding $$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!> padding""")
@Annotation($$$$"""padding $$$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!> padding""")
@Annotation($$$$$$$$"""padding $$$$$$$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!> padding""")

@Annotation("""padding $<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!> padding""")
@Annotation(<!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!> padding"""<!>)
@Annotation($$"""padding $$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!> padding""")
@Annotation($$$$"""padding $$$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!> padding""")
@Annotation($$$$$$$$"""padding $$$$$$$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!> padding""")

@Annotation("""padding ${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>} padding""")
@Annotation(<!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>} padding"""<!>)
@Annotation($$"""padding $${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>} padding""")
@Annotation($$$$"""padding $$$${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>} padding""")
@Annotation($$$$$$$$"""padding $$$$$$$${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>} padding""")

fun stringsWithInterpolationAsInvalidAnnotationArguments() {}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line

const val stringWithInterpolationAsInvalidConstantInitializer01 = "padding $<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!> padding"
const val stringWithInterpolationAsInvalidConstantInitializer02 = <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!> padding"<!>
const val stringWithInterpolationAsInvalidConstantInitializer03 = $$"padding $$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!> padding"
const val stringWithInterpolationAsInvalidConstantInitializer04 = $$$$"padding $$$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!> padding"
const val stringWithInterpolationAsInvalidConstantInitializer05 = $$$$$$$$"padding $$$$$$$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!> padding"

const val stringWithInterpolationAsInvalidConstantInitializer06 = "padding $<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!> padding"
const val stringWithInterpolationAsInvalidConstantInitializer07 = <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!> padding"<!>
const val stringWithInterpolationAsInvalidConstantInitializer08 = $$"padding $$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!> padding"
const val stringWithInterpolationAsInvalidConstantInitializer09 = $$$$"padding $$$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!> padding"
const val stringWithInterpolationAsInvalidConstantInitializer10 = $$$$$$$$"padding $$$$$$$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!> padding"

const val stringWithInterpolationAsInvalidConstantInitializer11 = "padding ${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>} padding"
const val stringWithInterpolationAsInvalidConstantInitializer12 = <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>} padding"<!>
const val stringWithInterpolationAsInvalidConstantInitializer13 = $$"padding $${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>} padding"
const val stringWithInterpolationAsInvalidConstantInitializer14 = $$$$"padding $$$${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>} padding"
const val stringWithInterpolationAsInvalidConstantInitializer15 = $$$$$$$$"padding $$$$$$$${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>} padding"


const val stringWithInterpolationAsInvalidConstantInitializer16 = """padding $<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!> padding"""
const val stringWithInterpolationAsInvalidConstantInitializer17 = <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!> padding"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer18 = $$"""padding $$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!> padding"""
const val stringWithInterpolationAsInvalidConstantInitializer19 = $$$$"""padding $$$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!> padding"""
const val stringWithInterpolationAsInvalidConstantInitializer20 = $$$$$$$$"""padding $$$$$$$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!> padding"""

const val stringWithInterpolationAsInvalidConstantInitializer21 = """padding $<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!> padding"""
const val stringWithInterpolationAsInvalidConstantInitializer22 = <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!> padding"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer23 = $$"""padding $$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!> padding"""
const val stringWithInterpolationAsInvalidConstantInitializer24 = $$$$"""padding $$$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!> padding"""
const val stringWithInterpolationAsInvalidConstantInitializer25 = $$$$$$$$"""padding $$$$$$$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!> padding"""

const val stringWithInterpolationAsInvalidConstantInitializer26 = """padding ${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>} padding"""
const val stringWithInterpolationAsInvalidConstantInitializer27 = <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>} padding"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer28 = $$"""padding $${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>} padding"""
const val stringWithInterpolationAsInvalidConstantInitializer29 = $$$$"""padding $$$${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>} padding"""
const val stringWithInterpolationAsInvalidConstantInitializer30 = $$$$$$$$"""padding $$$$$$$${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>} padding"""

/* GENERATED_FIR_TAGS: additiveExpression, annotationDeclaration, const, functionDeclaration, getter, integerLiteral,
primaryConstructor, propertyDeclaration, stringLiteral */
