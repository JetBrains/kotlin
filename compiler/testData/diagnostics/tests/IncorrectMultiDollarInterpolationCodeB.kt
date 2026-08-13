// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -MultiDollarInterpolation
// WITH_STDLIB

// COMPARE_WITH_LIGHT_TREE
// REASON: KT-68958

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun emptyInterpolation() {
    "${<!SYNTAX!><!>}"
    <!UNSUPPORTED_FEATURE{LT}!>$"${<!SYNTAX!><!>}"<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$"$${<!SYNTAX!><!>}"<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$"$$$${<!SYNTAX!><!>}"<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$$$$$"$$$$$$$${<!SYNTAX!><!>}"<!>

    """${<!SYNTAX!><!>}"""
    <!UNSUPPORTED_FEATURE{LT}!>$"""${<!SYNTAX!><!>}"""<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$"""$${<!SYNTAX!><!>}"""<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$"""$$$${<!SYNTAX!><!>}"""<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$$$$$"""$$$$$$$${<!SYNTAX!><!>}"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun blankInterpolation() {
    "${<!SYNTAX!><!>    }"
    <!UNSUPPORTED_FEATURE{LT}!>$"${<!SYNTAX!><!>    }"<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$"$${<!SYNTAX!><!>    }"<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$"$$$${<!SYNTAX!><!>    }"<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$$$$$"$$$$$$$${<!SYNTAX!><!>    }"<!>

    """${<!SYNTAX!><!>    }"""
    <!UNSUPPORTED_FEATURE{LT}!>$"""${<!SYNTAX!><!>    }"""<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$"""$${<!SYNTAX!><!>    }"""<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$"""$$$${<!SYNTAX!><!>    }"""<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$$$$$"""$$$$$$$${<!SYNTAX!><!>    }"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun linebreakInterpolation() {
    "${<!SYNTAX!><!>
    }"
    <!UNSUPPORTED_FEATURE{LT}!>$"${<!SYNTAX!><!>
    }"<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$"$${<!SYNTAX!><!>
    }"<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$"$$$${<!SYNTAX!><!>
    }"<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$$$$$"$$$$$$$${<!SYNTAX!><!>
    }"<!>

    """${<!SYNTAX!><!>
    }"""
    <!UNSUPPORTED_FEATURE{LT}!>$"""${<!SYNTAX!><!>
    }"""<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$"""$${<!SYNTAX!><!>
    }"""<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$"""$$$${<!SYNTAX!><!>
    }"""<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$$$$$"""$$$$$$$${<!SYNTAX!><!>
    }"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfUnresolvedReference() {
    "$<!UNRESOLVED_REFERENCE!>unresolved<!>"
    <!UNSUPPORTED_FEATURE!>$"$<!UNRESOLVED_REFERENCE!>unresolved<!>"<!>
    <!UNSUPPORTED_FEATURE!>$$"$$<!UNRESOLVED_REFERENCE!>unresolved<!>"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"$$$$<!UNRESOLVED_REFERENCE!>unresolved<!>"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"$$$$$$$$<!UNRESOLVED_REFERENCE!>unresolved<!>"<!>

    "$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"
    <!UNSUPPORTED_FEATURE!>$"$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"<!>
    <!UNSUPPORTED_FEATURE!>$$"$$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"$$$$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"$$$$$$$$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"<!>

    "${<!UNRESOLVED_REFERENCE!>unresolved<!>}"
    <!UNSUPPORTED_FEATURE!>$"${<!UNRESOLVED_REFERENCE!>unresolved<!>}"<!>
    <!UNSUPPORTED_FEATURE!>$$"$${<!UNRESOLVED_REFERENCE!>unresolved<!>}"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"$$$${<!UNRESOLVED_REFERENCE!>unresolved<!>}"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"$$$$$$$${<!UNRESOLVED_REFERENCE!>unresolved<!>}"<!>


    """$<!UNRESOLVED_REFERENCE!>unresolved<!>"""
    <!UNSUPPORTED_FEATURE!>$"""$<!UNRESOLVED_REFERENCE!>unresolved<!>"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""$$<!UNRESOLVED_REFERENCE!>unresolved<!>"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""$$$$<!UNRESOLVED_REFERENCE!>unresolved<!>"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""$$$$$$$$<!UNRESOLVED_REFERENCE!>unresolved<!>"""<!>

    """$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"""
    <!UNSUPPORTED_FEATURE!>$"""$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""$$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""$$$$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""$$$$$$$$<!UNRESOLVED_REFERENCE!>`unresolved`<!>"""<!>

    """${<!UNRESOLVED_REFERENCE!>unresolved<!>}"""
    <!UNSUPPORTED_FEATURE!>$"""${<!UNRESOLVED_REFERENCE!>unresolved<!>}"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""$${<!UNRESOLVED_REFERENCE!>unresolved<!>}"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""$$$${<!UNRESOLVED_REFERENCE!>unresolved<!>}"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""$$$$$$$${<!UNRESOLVED_REFERENCE!>unresolved<!>}"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfMisplacedDollar() {
    "$<!UNRESOLVED_REFERENCE!>`$`<!>"
    <!UNSUPPORTED_FEATURE!>$"$<!UNRESOLVED_REFERENCE!>`$`<!>"<!>
    <!UNSUPPORTED_FEATURE!>$$"$$<!UNRESOLVED_REFERENCE!>`$`<!>"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"$$$$<!UNRESOLVED_REFERENCE!>`$`<!>"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"$$$$$$$$<!UNRESOLVED_REFERENCE!>`$`<!>"<!>

    "${<!SYNTAX!><!>$<!SYNTAX!><!>}"
    <!UNSUPPORTED_FEATURE{LT}!>$"${<!SYNTAX!><!>$<!SYNTAX!><!>}"<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$"$${<!SYNTAX!><!>$<!SYNTAX!><!>}"<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$"$$$${<!SYNTAX!><!>$<!SYNTAX!><!>}"<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$$$$$"$$$$$$$${<!SYNTAX!><!>$<!SYNTAX!><!>}"<!>


    """$<!UNRESOLVED_REFERENCE!>`$`<!>"""
    <!UNSUPPORTED_FEATURE!>$"""$<!UNRESOLVED_REFERENCE!>`$`<!>"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""$$<!UNRESOLVED_REFERENCE!>`$`<!>"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""$$$$<!UNRESOLVED_REFERENCE!>`$`<!>"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""$$$$$$$$<!UNRESOLVED_REFERENCE!>`$`<!>"""<!>

    """${<!SYNTAX!><!>$<!SYNTAX!><!>}"""
    <!UNSUPPORTED_FEATURE{LT}!>$"""${<!SYNTAX!><!>$<!SYNTAX!><!>}"""<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$"""$${<!SYNTAX!><!>$<!SYNTAX!><!>}"""<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$"""$$$${<!SYNTAX!><!>$<!SYNTAX!><!>}"""<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$$$$$"""$$$$$$$${<!SYNTAX!><!>$<!SYNTAX!><!>}"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfMisplacedInterpolation() {
    "$<!UNRESOLVED_REFERENCE!>`$value`<!>"
    <!UNSUPPORTED_FEATURE!>$"$<!UNRESOLVED_REFERENCE!>`$value`<!>"<!>
    <!UNSUPPORTED_FEATURE!>$$"$$<!UNRESOLVED_REFERENCE!>`$$value`<!>"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"$$$$<!UNRESOLVED_REFERENCE!>`$$$$value`<!>"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"$$$$$$$$<!UNRESOLVED_REFERENCE!>`$$$$$$$$value`<!>"<!>

    "${<!SYNTAX!><!>$value<!SYNTAX!><!>}"
    <!UNSUPPORTED_FEATURE{LT}!>$"${<!SYNTAX!><!>$value<!SYNTAX!><!>}"<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$"$${<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>}"<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$"$$$${<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>}"<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$$$$$"$$$$$$$${<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>}"<!>


    """$<!UNRESOLVED_REFERENCE!>`$value`<!>"""
    <!UNSUPPORTED_FEATURE!>$"""$<!UNRESOLVED_REFERENCE!>`$value`<!>"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""$$<!UNRESOLVED_REFERENCE!>`$$value`<!>"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""$$$$<!UNRESOLVED_REFERENCE!>`$$$$value`<!>"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""$$$$$$$$<!UNRESOLVED_REFERENCE!>`$$$$$$$$value`<!>"""<!>

    """${<!SYNTAX!><!>$value<!SYNTAX!><!>}"""
    <!UNSUPPORTED_FEATURE{LT}!>$"""${<!SYNTAX!><!>$value<!SYNTAX!><!>}"""<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$"""$${<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>}"""<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$"""$$$${<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>}"""<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$$$$$"""$$$$$$$${<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$<!SYNTAX!><!>$value<!SYNTAX!><!>}"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfIncorrectExpression() {
    "${42 +<!SYNTAX!><!>}"
    <!UNSUPPORTED_FEATURE{LT}!>$"${42 +<!SYNTAX!><!>}"<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$"$${42 +<!SYNTAX!><!>}"<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$"$$$${42 +<!SYNTAX!><!>}"<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$$$$$"$$$$$$$${42 +<!SYNTAX!><!>}"<!>

    """${42 +<!SYNTAX!><!>}"""
    <!UNSUPPORTED_FEATURE{LT}!>$"""${42 +<!SYNTAX!><!>}"""<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$"""$${42 +<!SYNTAX!><!>}"""<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$"""$$$${42 +<!SYNTAX!><!>}"""<!>
    <!UNSUPPORTED_FEATURE{LT}!>$$$$$$$$"""$$$$$$$${42 +<!SYNTAX!><!>}"""<!>
}

val runTimeConstant get() = 42

@Repeatable annotation class Annotation(val value: String)

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line

@Annotation("$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>")
@Annotation(<!UNSUPPORTED_FEATURE!>$"$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>"<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$"$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>"<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$"$$$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>"<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$$$$$"$$$$$$$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>"<!>)

@Annotation("$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!>")
@Annotation(<!UNSUPPORTED_FEATURE!>$"$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!>"<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$"$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!>"<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$"$$$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!>"<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$$$$$"$$$$$$$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!>"<!>)

@Annotation("${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>}")
@Annotation(<!UNSUPPORTED_FEATURE!>$"${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>}"<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$"$${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>}"<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$"$$$${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>}"<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$$$$$"$$$$$$$${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>}"<!>)


@Annotation("""$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>""")
@Annotation(<!UNSUPPORTED_FEATURE!>$"""$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>"""<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$"""$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>"""<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$"""$$$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>"""<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$$$$$"""$$$$$$$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>"""<!>)

@Annotation("""$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!>""")
@Annotation(<!UNSUPPORTED_FEATURE!>$"""$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!>"""<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$"""$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!>"""<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$"""$$$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!>"""<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$$$$$"""$$$$$$$$<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>`runTimeConstant`<!>"""<!>)

@Annotation("""${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>}""")
@Annotation(<!UNSUPPORTED_FEATURE!>$"""${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>}"""<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$"""$${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>}"""<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$"""$$$${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>}"""<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$$$$$"""$$$$$$$${0 + <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>runTimeConstant<!>}"""<!>)

fun stringsWithInterpolationAsInvalidAnnotationArguments() {}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line

const val stringWithInterpolationAsInvalidConstantInitializer01 = "$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>"
const val stringWithInterpolationAsInvalidConstantInitializer02 = <!UNSUPPORTED_FEATURE!>$"$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>"<!>
const val stringWithInterpolationAsInvalidConstantInitializer03 = <!UNSUPPORTED_FEATURE!>$$"$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>"<!>
const val stringWithInterpolationAsInvalidConstantInitializer04 = <!UNSUPPORTED_FEATURE!>$$$$"$$$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>"<!>
const val stringWithInterpolationAsInvalidConstantInitializer05 = <!UNSUPPORTED_FEATURE!>$$$$$$$$"$$$$$$$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>"<!>

const val stringWithInterpolationAsInvalidConstantInitializer06 = "$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!>"
const val stringWithInterpolationAsInvalidConstantInitializer07 = <!UNSUPPORTED_FEATURE!>$"$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!>"<!>
const val stringWithInterpolationAsInvalidConstantInitializer08 = <!UNSUPPORTED_FEATURE!>$$"$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!>"<!>
const val stringWithInterpolationAsInvalidConstantInitializer09 = <!UNSUPPORTED_FEATURE!>$$$$"$$$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!>"<!>
const val stringWithInterpolationAsInvalidConstantInitializer10 = <!UNSUPPORTED_FEATURE!>$$$$$$$$"$$$$$$$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!>"<!>

const val stringWithInterpolationAsInvalidConstantInitializer11 = "${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>}"
const val stringWithInterpolationAsInvalidConstantInitializer12 = <!UNSUPPORTED_FEATURE!>$"${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>}"<!>
const val stringWithInterpolationAsInvalidConstantInitializer13 = <!UNSUPPORTED_FEATURE!>$$"$${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>}"<!>
const val stringWithInterpolationAsInvalidConstantInitializer14 = <!UNSUPPORTED_FEATURE!>$$$$"$$$${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>}"<!>
const val stringWithInterpolationAsInvalidConstantInitializer15 = <!UNSUPPORTED_FEATURE!>$$$$$$$$"$$$$$$$${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>}"<!>


const val stringWithInterpolationAsInvalidConstantInitializer16 = """$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>"""
const val stringWithInterpolationAsInvalidConstantInitializer17 = <!UNSUPPORTED_FEATURE!>$"""$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer18 = <!UNSUPPORTED_FEATURE!>$$"""$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer19 = <!UNSUPPORTED_FEATURE!>$$$$"""$$$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer20 = <!UNSUPPORTED_FEATURE!>$$$$$$$$"""$$$$$$$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>"""<!>

const val stringWithInterpolationAsInvalidConstantInitializer21 = """$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!>"""
const val stringWithInterpolationAsInvalidConstantInitializer22 = <!UNSUPPORTED_FEATURE!>$"""$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!>"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer23 = <!UNSUPPORTED_FEATURE!>$$"""$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!>"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer24 = <!UNSUPPORTED_FEATURE!>$$$$"""$$$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!>"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer25 = <!UNSUPPORTED_FEATURE!>$$$$$$$$"""$$$$$$$$<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>`runTimeConstant`<!>"""<!>

const val stringWithInterpolationAsInvalidConstantInitializer26 = """${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>}"""
const val stringWithInterpolationAsInvalidConstantInitializer27 = <!UNSUPPORTED_FEATURE!>$"""${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>}"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer28 = <!UNSUPPORTED_FEATURE!>$$"""$${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>}"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer29 = <!UNSUPPORTED_FEATURE!>$$$$"""$$$${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>}"""<!>
const val stringWithInterpolationAsInvalidConstantInitializer30 = <!UNSUPPORTED_FEATURE!>$$$$$$$$"""$$$$$$$${0 + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>runTimeConstant<!>}"""<!>

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

/* GENERATED_FIR_TAGS: additiveExpression, annotationDeclaration, const, functionDeclaration, getter, integerLiteral,
primaryConstructor, propertyDeclaration, stringLiteral */
