// LANGUAGE: +MultiDollarInterpolation
// WITH_EXTENDED_CHECKERS
// DIAGNOSTICS: -warnings +REDUNDANT_INTERPOLATION_PREFIX
// WITH_STDLIB

// FIR_DUMP
// REASON: KT-68971

// ISSUE: KT-69062
// ISSUE: KT-68957
// ISSUE: KT-68969

// interpolation prefix length: 0, 1, 2, 4, 8
// string literal kinds: single-line, multi-line
fun emptyStrings() {
    ""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$""<!>

    """"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$""""""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$""""""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$""""""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$""""""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// string literal kinds: single-line, multi-line
fun stringsWithoutInterpolation() {
    "padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$"padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$"padding"<!>

    """padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"""padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$"""padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$"""padding"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun dollarInStrings() {
    "padding $ padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $ padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"padding $ padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$"padding $ padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$"padding $ padding"<!>

    """padding $ padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $ padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"""padding $ padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$"""padding $ padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$"""padding $ padding"""<!>


    "padding $ padding $text"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $ padding $text"<!>
    $$"padding $ padding $$text"
    $$$$"padding $ padding $$$$text"
    $$$$$$$$"padding $ padding $$$$$$$$text"

    """padding $ padding $text"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $ padding $text"""<!>
    $$"""padding $ padding $$text"""
    $$$$"""padding $ padding $$$$text"""
    $$$$$$$$"""padding $ padding $$$$$$$$text"""
}

// interpolation prefix length: 2, 4, 8
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun dollarsInStringsA() {
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"padding $$ padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$"padding $$$$ padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$"padding $$$$$$$$ padding"<!>

    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"""padding $$ padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$"""padding $$$$ padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$"""padding $$$$$$$$ padding"""<!>


    $$"padding $$ padding $$text"
    $$$$"padding $$$$ padding $$$$text"
    $$$$$$$$"padding $$$$$$$$ padding $$$$$$$$text"

    $$"""padding $$ padding $$text"""
    $$$$"""padding $$$$ padding $$$$text"""
    $$$$$$$$"""padding $$$$$$$$ padding $$$$$$$$text"""
}

// interpolation prefix length: 2, 4, 8
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun dollarsInStringsB() {
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"padding $ padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$"padding $$$ padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$"padding $$$$$$$ padding"<!>

    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"""padding $ padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$"""padding $$$ padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$"""padding $$$$$$$ padding"""<!>


    $$"padding $ padding $$text"
    $$$$"padding $$$ padding $$$$text"
    $$$$$$$$"padding $$$$$$$ padding $$$$$$$$text"

    $$"""padding $ padding $$text"""
    $$$$"""padding $$$ padding $$$$text"""
    $$$$$$$$"""padding $$$$$$$ padding $$$$$$$$text"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun dollarsInStringsC() {
    "padding $$ padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $$ padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"padding $$$ padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$"padding $$$$$ padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$"padding $$$$$$$$$ padding"<!>

    """padding $$ padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $$ padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"""padding $$$ padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$"""padding $$$$$ padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$"""padding $$$$$$$$$ padding"""<!>


    "padding $$ padding $text"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $$ padding $text"<!>
    $$"padding $$$ padding $$text"
    $$$$"padding $$$$$ padding $$$$text"
    $$$$$$$$"padding $$$$$$$$$ padding $$$$$$$$text"

    """padding $$ padding $text"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $$ padding $text"""<!>
    $$"""padding $$$ padding $$text"""
    $$$$"""padding $$$$$ padding $$$$text"""
    $$$$$$$$"""padding $$$$$$$$$ padding $$$$$$$$text"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun dollarsWithoutInterpolationOfSimpleIdentifierA() {
    "padding $ value padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $ value padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"padding $$ value padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$"padding $$$$ value padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$"padding $$$$$$$$ value padding"<!>

    """padding $ value padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $ value padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"""padding $$ value padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$"""padding $$$$ value padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$"""padding $$$$$$$$ value padding"""<!>


    "padding $ value padding $text"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $ value padding $text"<!>
    $$"padding $$ value padding $$text"
    $$$$"padding $$$$ value padding $$$$text"
    $$$$$$$$"padding $$$$$$$$ value padding $$$$$$$$text"

    """padding $ value padding $text"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $ value padding $text"""<!>
    $$"""padding $$ value padding $$text"""
    $$$$"""padding $$$$ value padding $$$$text"""
    $$$$$$$$"""padding $$$$$$$$ value padding $$$$$$$$text"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun dollarsWithoutInterpolationOfSimpleIdentifierB() {
    "padding $-value padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $-value padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"padding $$-value padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$"padding $$$$-value padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$"padding $$$$$$$$-value padding"<!>

    """padding $-value padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $-value padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"""padding $$-value padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$"""padding $$$$-value padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$"""padding $$$$$$$$-value padding"""<!>


    "padding $-value padding $text"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $-value padding $text"<!>
    $$"padding $$-value padding $$text"
    $$$$"padding $$$$-value padding $$$$text"
    $$$$$$$$"padding $$$$$$$$-value padding $$$$$$$$text"

    """padding $-value padding $text"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $-value padding $text"""<!>
    $$"""padding $$-value padding $$text"""
    $$$$"""padding $$$$-value padding $$$$text"""
    $$$$$$$$"""padding $$$$$$$$-value padding $$$$$$$$text"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun dollarsWithoutInterpolationOfIdentifierInBackticks() {
    "padding $`` padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $`` padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"padding $$`` padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$"padding $$$$`` padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$"padding $$$$$$$$`` padding"<!>

    """padding $`` padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $`` padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"""padding $$`` padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$"""padding $$$$`` padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$"""padding $$$$$$$$`` padding"""<!>


    "padding $`` padding $text"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $`` padding $text"<!>
    $$"padding $$`` padding $$text"
    $$$$"padding $$$$`` padding $$$$text"
    $$$$$$$$"padding $$$$$$$$`` padding $$$$$$$$text"

    """padding $`` padding $text"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $`` padding $text"""<!>
    $$"""padding $$`` padding $$text"""
    $$$$"""padding $$$$`` padding $$$$text"""
    $$$$$$$$"""padding $$$$$$$$`` padding $$$$$$$$text"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun dollarsWithoutInterpolationOfArbitraryExpression() {
    "padding $ {0 + value} padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $ {0 + value} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"padding $$ {0 + value} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$"padding $$$$ {0 + value} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$"padding $$$$$$$$ {0 + value} padding"<!>

    """padding $ {0 + value} padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $ {0 + value} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"""padding $$ {0 + value} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$"""padding $$$$ {0 + value} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$$$$$$$"""padding $$$$$$$$ {0 + value} padding"""<!>


    "padding $ {0 + value} padding $text"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $ {0 + value} padding $text"<!>
    $$"padding $$ {0 + value} padding $$text"
    $$$$"padding $$$$ {0 + value} padding $$$$text"
    $$$$$$$$"padding $$$$$$$$ {0 + value} padding $$$$$$$$text"

    """padding $ {0 + value} padding $text"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $ {0 + value} padding $text"""<!>
    $$"""padding $$ {0 + value} padding $$text"""
    $$$$"""padding $$$$ {0 + value} padding $$$$text"""
    $$$$$$$$"""padding $$$$$$$$ {0 + value} padding $$$$$$$$text"""
}

// interpolation prefix length: 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun insufficientDollarForInterpolation() {
    $$"padding $value padding"
    $$$$"padding $value padding"
    $$$$$$$$"padding $value padding"

    $$"padding $`value` padding"
    $$$$"padding $`value` padding"
    $$$$$$$$"padding $`value` padding"

    $$"padding ${0 + value} padding"
    $$$$"padding ${0 + value} padding"
    $$$$$$$$"padding ${0 + value} padding"


    $$"""padding $value padding"""
    $$$$"""padding $value padding"""
    $$$$$$$$"""padding $value padding"""

    $$"""padding $`value` padding"""
    $$$$"""padding $`value` padding"""
    $$$$$$$$"""padding $`value` padding"""

    $$"""padding ${0 + value} padding"""
    $$$$"""padding ${0 + value} padding"""
    $$$$$$$$"""padding ${0 + value} padding"""



    $$"padding $value padding $$text"
    $$$$"padding $value padding $$$$text"
    $$$$$$$$"padding $value padding $$$$$$$$text"

    $$"padding $`value` padding $$text"
    $$$$"padding $`value` padding $$$$text"
    $$$$$$$$"padding $`value` padding $$$$$$$$text"

    $$"padding ${0 + value} padding $$text"
    $$$$"padding ${0 + value} padding $$$$text"
    $$$$$$$$"padding ${0 + value} padding $$$$$$$$text"


    $$"""padding $value padding $$text"""
    $$$$"""padding $value padding $$$$text"""
    $$$$$$$$"""padding $value padding $$$$$$$$text"""

    $$"""padding $`value` padding $$text"""
    $$$$"""padding $`value` padding $$$$text"""
    $$$$$$$$"""padding $`value` padding $$$$$$$$text"""

    $$"""padding ${0 + value} padding $$text"""
    $$$$"""padding ${0 + value} padding $$$$text"""
    $$$$$$$$"""padding ${0 + value} padding $$$$$$$$text"""
}

// interpolation prefix length: 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun insufficientDollarsForInterpolation() {
    $$$$"padding $$$value padding"
    $$$$$$$$"padding $$$$$$$value padding"

    $$$$"padding $$$`value` padding"
    $$$$$$$$"padding $$$$$$$`value` padding"

    $$$$"padding $$${0 + value} padding"
    $$$$$$$$"padding $$$$$$${0 + value} padding"


    $$$$"""padding $$$value padding"""
    $$$$$$$$"""padding $$$$$$$value padding"""

    $$$$"""padding $$$`value` padding"""
    $$$$$$$$"""padding $$$$$$$`value` padding"""

    $$$$"""padding $$${0 + value} padding"""
    $$$$$$$$"""padding $$$$$$${0 + value} padding"""



    $$$$"padding $$$value padding $$$$text"
    $$$$$$$$"padding $$$$$$$value padding $$$$$$$$text"

    $$$$"padding $$$`value` padding $$$$text"
    $$$$$$$$"padding $$$$$$$`value` padding $$$$$$$$text"

    $$$$"padding $$${0 + value} padding $$$$text"
    $$$$$$$$"padding $$$$$$${0 + value} padding $$$$$$$$text"


    $$$$"""padding $$$value padding $$$$text"""
    $$$$$$$$"""padding $$$$$$$value padding $$$$$$$$text"""

    $$$$"""padding $$$`value` padding $$$$text"""
    $$$$$$$$"""padding $$$$$$$`value` padding $$$$$$$$text"""

    $$$$"""padding $$${0 + value} padding $$$$text"""
    $$$$$$$$"""padding $$$$$$${0 + value} padding $$$$$$$$text"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line
// interpolation as padding: no, yes
fun escapedDollarInInterpolationPrefix() {
    "padding \$value padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding \$value padding"<!>
    $$"padding \$$value padding"
    $$$$"padding \$$$$value padding"
    $$$$$$$$"padding \$$$$$$$$value padding"

    "padding \$`value` padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding \$`value` padding"<!>
    $$"padding \$$`value` padding"
    $$$$"padding \$$$$`value` padding"
    $$$$$$$$"padding \$$$$$$$$`value` padding"

    "padding \${0 + value} padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding \${0 + value} padding"<!>
    $$"padding \$${0 + value} padding"
    $$$$"padding \$$$${0 + value} padding"
    $$$$$$$$"padding \$$$$$$$${0 + value} padding"


    "padding \$value padding $text"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding \$value padding $text"<!>
    $$"padding \$$value padding $$text"
    $$$$"padding \$$$$value padding $$$$text"
    $$$$$$$$"padding \$$$$$$$$value padding $$$$$$$$text"

    "padding \$`value` padding $text"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding \$`value` padding $text"<!>
    $$"padding \$$`value` padding $$text"
    $$$$"padding \$$$$`value` padding $$$$text"
    $$$$$$$$"padding \$$$$$$$$`value` padding $$$$$$$$text"

    "padding \${0 + value} padding $text"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding \${0 + value} padding $text"<!>
    $$"padding \$${0 + value} padding $$text"
    $$$$"padding \$$$${0 + value} padding $$$$text"
    $$$$$$$$"padding \$$$$$$$${0 + value} padding $$$$$$$$text"
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun stringsWithRedundantInterpolation() {
    "$text"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"$text"<!>
    $$"$$text"
    $$$$"$$$$text"
    $$$$$$$$"$$$$$$$$text"

    "$`text`"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"$`text`"<!>
    $$"$$`text`"
    $$$$"$$$$`text`"
    $$$$$$$$"$$$$$$$$`text`"

    "${"" + text}"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"${"" + text}"<!>
    $$"$${"" + text}"
    $$$$"$$$${"" + text}"
    $$$$$$$$"$$$$$$$${"" + text}"


    """$text"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""$text"""<!>
    $$"""$$text"""
    $$$$"""$$$$text"""
    $$$$$$$$"""$$$$$$$$text"""

    """$`text`"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""$`text`"""<!>
    $$"""$$`text`"""
    $$$$"""$$$$`text`"""
    $$$$$$$$"""$$$$$$$$`text`"""

    """${"" + text}"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""${"" + text}"""<!>
    $$"""$${"" + text}"""
    $$$$"""$$$${"" + text}"""
    $$$$$$$$"""$$$$$$$${"" + text}"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun stringsWithInterpolationA() {
    "padding $text padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $text padding"<!>
    $$"padding $$text padding"
    $$$$"padding $$$$text padding"
    $$$$$$$$"padding $$$$$$$$text padding"

    "padding $`text` padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $`text` padding"<!>
    $$"padding $$`text` padding"
    $$$$"padding $$$$`text` padding"
    $$$$$$$$"padding $$$$$$$$`text` padding"

    "padding ${"" + text} padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${"" + text} padding"<!>
    $$"padding $${"" + text} padding"
    $$$$"padding $$$${"" + text} padding"
    $$$$$$$$"padding $$$$$$$${"" + text} padding"


    """padding $text padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $text padding"""<!>
    $$"""padding $$text padding"""
    $$$$"""padding $$$$text padding"""
    $$$$$$$$"""padding $$$$$$$$text padding"""

    """padding $`text` padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $`text` padding"""<!>
    $$"""padding $$`text` padding"""
    $$$$"""padding $$$$`text` padding"""
    $$$$$$$$"""padding $$$$$$$$`text` padding"""

    """padding ${"" + text} padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${"" + text} padding"""<!>
    $$"""padding $${"" + text} padding"""
    $$$$"""padding $$$${"" + text} padding"""
    $$$$$$$$"""padding $$$$$$$${"" + text} padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun stringsWithInterpolationB() {
    "$value"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"$value"<!>
    $$"$$value"
    $$$$"$$$$value"
    $$$$$$$$"$$$$$$$$value"

    "$`value`"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"$`value`"<!>
    $$"$$`value`"
    $$$$"$$$$`value`"
    $$$$$$$$"$$$$$$$$`value`"

    "${0 + value}"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"${0 + value}"<!>
    $$"$${0 + value}"
    $$$$"$$$${0 + value}"
    $$$$$$$$"$$$$$$$${0 + value}"


    """$value"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""$value"""<!>
    $$"""$$value"""
    $$$$"""$$$$value"""
    $$$$$$$$"""$$$$$$$$value"""

    """$`value`"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""$`value`"""<!>
    $$"""$$`value`"""
    $$$$"""$$$$`value`"""
    $$$$$$$$"""$$$$$$$$`value`"""

    """${0 + value}"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""${0 + value}"""<!>
    $$"""$${0 + value}"""
    $$$$"""$$$${0 + value}"""
    $$$$$$$$"""$$$$$$$${0 + value}"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfDollarSymbolA() {
    "padding ${'$'}value"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${'$'}value"<!>
    $$"padding $${'$'}value"
    $$$$"padding $$$${'$'}value"
    $$$$$$$$"padding $$$$$$$${'$'}value"

    """padding ${'$'}value"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${'$'}value"""<!>
    $$"""padding $${'$'}value"""
    $$$$"""padding $$$${'$'}value"""
    $$$$$$$$"""padding $$$$$$$${'$'}value"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfDollarSymbolB() {
    "padding ${'$'}$value padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${'$'}$value padding"<!>
    $$"padding $${'$'}$$value padding"
    $$$$"padding $$$${'$'}$$$$value padding"
    $$$$$$$$"padding $$$$$$$${'$'}$$$$$$$$value padding"

    """padding ${'$'}$value padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${'$'}$value padding"""<!>
    $$"""padding $${'$'}$$value padding"""
    $$$$"""padding $$$${'$'}$$$$value padding"""
    $$$$$$$$"""padding $$$$$$$${'$'}$$$$$$$$value padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of identifier in backticks
// string literal kinds: single-line, multi-line
fun interpolationOfDollarClassifierA() {
    "padding $`$`value"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $`$`value"<!>
    $$"padding $$`$`value"
    $$$$"padding $$$$`$`value"
    $$$$$$$$"padding $$$$$$$$`$`value"

    """padding $`$`value"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $`$`value"""<!>
    $$"""padding $$`$`value"""
    $$$$"""padding $$$$`$`value"""
    $$$$$$$$"""padding $$$$$$$$`$`value"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of identifier in backticks
// string literal kinds: single-line, multi-line
fun interpolationOfDollarClassifierB() {
    "padding $`$`$value padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $`$`$value padding"<!>
    $$"padding $$`$`$$value padding"
    $$$$"padding $$$$`$`$$$$value padding"
    $$$$$$$$"padding $$$$$$$$`$`$$$$$$$$value padding"

    """padding $`$`$value padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $`$`$value padding"""<!>
    $$"""padding $$`$`$$value padding"""
    $$$$"""padding $$$$`$`$$$$value padding"""
    $$$$$$$$"""padding $$$$$$$$`$`$$$$$$$$value padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun excessiveDollarsForInterpolation() {
    "padding $$value padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $$value padding"<!>
    $$"padding $$$value padding"
    $$$$"padding $$$$$value padding"
    $$$$$$$$"padding $$$$$$$$$value padding"

    "padding $$`value` padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $$`value` padding"<!>
    $$"padding $$$`value` padding"
    $$$$"padding $$$$$`value` padding"
    $$$$$$$$"padding $$$$$$$$$`value` padding"

    "padding $${0 + value} padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $${0 + value} padding"<!>
    $$"padding $$${0 + value} padding"
    $$$$"padding $$$$${0 + value} padding"
    $$$$$$$$"padding $$$$$$$$${0 + value} padding"


    """padding $$value padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $$value padding"""<!>
    $$"""padding $$$value padding"""
    $$$$"""padding $$$$$value padding"""
    $$$$$$$$"""padding $$$$$$$$$value padding"""

    """padding $$`value` padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $$`value` padding"""<!>
    $$"""padding $$$`value` padding"""
    $$$$"""padding $$$$$`value` padding"""
    $$$$$$$$"""padding $$$$$$$$$`value` padding"""

    """padding $${0 + value} padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $${0 + value} padding"""<!>
    $$"""padding $$${0 + value} padding"""
    $$$$"""padding $$$$${0 + value} padding"""
    $$$$$$$$"""padding $$$$$$$$${0 + value} padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds A: of simple identifier, of identifier in backticks, of arbitrary expression
// interpolation kinds B: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun sequentialInterpolation() {
    "padding $value$text padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $value$text padding"<!>
    $$"padding $$value$$text padding"
    $$$$"padding $$$$value$$$$text padding"
    $$$$$$$$"padding $$$$$$$$value$$$$$$$$text padding"

    "padding $`value`$text padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $`value`$text padding"<!>
    $$"padding $$`value`$$text padding"
    $$$$"padding $$$$`value`$$$$text padding"
    $$$$$$$$"padding $$$$$$$$`value`$$$$$$$$text padding"

    "padding ${0 + value}$text padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${0 + value}$text padding"<!>
    $$"padding $${0 + value}$$text padding"
    $$$$"padding $$$${0 + value}$$$$text padding"
    $$$$$$$$"padding $$$$$$$${0 + value}$$$$$$$$text padding"


    "padding $value$`text` padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $value$`text` padding"<!>
    $$"padding $$value$$`text` padding"
    $$$$"padding $$$$value$$$$`text` padding"
    $$$$$$$$"padding $$$$$$$$value$$$$$$$$`text` padding"

    "padding $`value`$`text` padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $`value`$`text` padding"<!>
    $$"padding $$`value`$$`text` padding"
    $$$$"padding $$$$`value`$$$$`text` padding"
    $$$$$$$$"padding $$$$$$$$`value`$$$$$$$$`text` padding"

    "padding ${0 + value}$`text` padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${0 + value}$`text` padding"<!>
    $$"padding $${0 + value}$$`text` padding"
    $$$$"padding $$$${0 + value}$$$$`text` padding"
    $$$$$$$$"padding $$$$$$$${0 + value}$$$$$$$$`text` padding"


    "padding $value${"" + text} padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $value${"" + text} padding"<!>
    $$"padding $$value$${"" + text} padding"
    $$$$"padding $$$$value$$$${"" + text} padding"
    $$$$$$$$"padding $$$$$$$$value$$$$$$$${"" + text} padding"

    "padding $`value`${"" + text} padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $`value`${"" + text} padding"<!>
    $$"padding $$`value`$${"" + text} padding"
    $$$$"padding $$$$`value`$$$${"" + text} padding"
    $$$$$$$$"padding $$$$$$$$`value`$$$$$$$${"" + text} padding"

    "padding ${0 + value}${"" + text} padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${0 + value}${"" + text} padding"<!>
    $$"padding $${0 + value}$${"" + text} padding"
    $$$$"padding $$$${0 + value}$$$${"" + text} padding"
    $$$$$$$$"padding $$$$$$$${0 + value}$$$$$$$${"" + text} padding"



    """padding $value$text padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $value$text padding"""<!>
    $$"""padding $$value$$text padding"""
    $$$$"""padding $$$$value$$$$text padding"""
    $$$$$$$$"""padding $$$$$$$$value$$$$$$$$text padding"""

    """padding $`value`$text padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $`value`$text padding"""<!>
    $$"""padding $$`value`$$text padding"""
    $$$$"""padding $$$$`value`$$$$text padding"""
    $$$$$$$$"""padding $$$$$$$$`value`$$$$$$$$text padding"""

    """padding ${0 + value}$text padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${0 + value}$text padding"""<!>
    $$"""padding $${0 + value}$$text padding"""
    $$$$"""padding $$$${0 + value}$$$$text padding"""
    $$$$$$$$"""padding $$$$$$$${0 + value}$$$$$$$$text padding"""


    """padding $value$`text` padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $value$`text` padding"""<!>
    $$"""padding $$value$$`text` padding"""
    $$$$"""padding $$$$value$$$$`text` padding"""
    $$$$$$$$"""padding $$$$$$$$value$$$$$$$$`text` padding"""

    """padding $`value`$`text` padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $`value`$`text` padding"""<!>
    $$"""padding $$`value`$$`text` padding"""
    $$$$"""padding $$$$`value`$$$$`text` padding"""
    $$$$$$$$"""padding $$$$$$$$`value`$$$$$$$$`text` padding"""

    """padding ${0 + value}$`text` padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${0 + value}$`text` padding"""<!>
    $$"""padding $${0 + value}$$`text` padding"""
    $$$$"""padding $$$${0 + value}$$$$`text` padding"""
    $$$$$$$$"""padding $$$$$$$${0 + value}$$$$$$$$`text` padding"""


    """padding $value${"" + text} padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $value${"" + text} padding"""<!>
    $$"""padding $$value$${"" + text} padding"""
    $$$$"""padding $$$$value$$$${"" + text} padding"""
    $$$$$$$$"""padding $$$$$$$$value$$$$$$$${"" + text} padding"""

    """padding $`value`${"" + text} padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $`value`${"" + text} padding"""<!>
    $$"""padding $$`value`$${"" + text} padding"""
    $$$$"""padding $$$$`value`$$$${"" + text} padding"""
    $$$$$$$$"""padding $$$$$$$$`value`$$$$$$$${"" + text} padding"""

    """padding ${0 + value}${"" + text} padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${0 + value}${"" + text} padding"""<!>
    $$"""padding $${0 + value}$${"" + text} padding"""
    $$$$"""padding $$$${0 + value}$$$${"" + text} padding"""
    $$$$$$$$"""padding $$$$$$$${0 + value}$$$$$$$${"" + text} padding"""
}

// inner interpolation prefix length: 0, 1, 2, 4, 8
// outer interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// inner string literal kinds: single-line, multi-line
// outer string literal kinds: single-line, multi-line
fun nestedInterpolation() {
    "padding ${"more$text"} padding"
    "padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$text"<!>} padding"
    "padding ${$$"more$$text"} padding"
    "padding ${$$$$"more$$$$text"} padding"
    "padding ${$$$$$$$$"more$$$$$$$$text"} padding"

    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${"more$text"} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$text"<!>} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${$$"more$$text"} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${$$$$"more$$$$text"} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${$$$$$$$$"more$$$$$$$$text"} padding"<!>

    $$"padding $${"more$text"} padding"
    $$"padding $${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$text"<!>} padding"
    $$"padding $${$$"more$$text"} padding"
    $$"padding $${$$$$"more$$$$text"} padding"
    $$"padding $${$$$$$$$$"more$$$$$$$$text"} padding"

    $$$$"padding $$$${"more$text"} padding"
    $$$$"padding $$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$text"<!>} padding"
    $$$$"padding $$$${$$"more$$text"} padding"
    $$$$"padding $$$${$$$$"more$$$$text"} padding"
    $$$$"padding $$$${$$$$$$$$"more$$$$$$$$text"} padding"

    $$$$$$$$"padding $$$$$$$${"more$text"} padding"
    $$$$$$$$"padding $$$$$$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$text"<!>} padding"
    $$$$$$$$"padding $$$$$$$${$$"more$$text"} padding"
    $$$$$$$$"padding $$$$$$$${$$$$"more$$$$text"} padding"
    $$$$$$$$"padding $$$$$$$${$$$$$$$$"more$$$$$$$$text"} padding"


    "padding ${"more$`text`"} padding"
    "padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$`text`"<!>} padding"
    "padding ${$$"more$$`text`"} padding"
    "padding ${$$$$"more$$$$`text`"} padding"
    "padding ${$$$$$$$$"more$$$$$$$$`text`"} padding"

    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${"more$`text`"} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$`text`"<!>} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${$$"more$$`text`"} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${$$$$"more$$$$`text`"} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${$$$$$$$$"more$$$$$$$$`text`"} padding"<!>

    $$"padding $${"more$`text`"} padding"
    $$"padding $${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$`text`"<!>} padding"
    $$"padding $${$$"more$$`text`"} padding"
    $$"padding $${$$$$"more$$$$`text`"} padding"
    $$"padding $${$$$$$$$$"more$$$$$$$$`text`"} padding"

    $$$$"padding $$$${"more$`text`"} padding"
    $$$$"padding $$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$`text`"<!>} padding"
    $$$$"padding $$$${$$"more$$`text`"} padding"
    $$$$"padding $$$${$$$$"more$$$$`text`"} padding"
    $$$$"padding $$$${$$$$$$$$"more$$$$$$$$`text`"} padding"

    $$$$$$$$"padding $$$$$$$${"more$`text`"} padding"
    $$$$$$$$"padding $$$$$$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$`text`"<!>} padding"
    $$$$$$$$"padding $$$$$$$${$$"more$$`text`"} padding"
    $$$$$$$$"padding $$$$$$$${$$$$"more$$$$`text`"} padding"
    $$$$$$$$"padding $$$$$$$${$$$$$$$$"more$$$$$$$$`text`"} padding"


    "padding ${"more${"" + text}"} padding"
    "padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more${"" + text}"<!>} padding"
    "padding ${$$"more$${"" + text}"} padding"
    "padding ${$$$$"more$$$${"" + text}"} padding"
    "padding ${$$$$$$$$"more$$$$$$$${"" + text}"} padding"

    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${"more${"" + text}"} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more${"" + text}"<!>} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${$$"more$${"" + text}"} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${$$$$"more$$$${"" + text}"} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${$$$$$$$$"more$$$$$$$${"" + text}"} padding"<!>

    $$"padding $${"more${"" + text}"} padding"
    $$"padding $${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more${"" + text}"<!>} padding"
    $$"padding $${$$"more$${"" + text}"} padding"
    $$"padding $${$$$$"more$$$${"" + text}"} padding"
    $$"padding $${$$$$$$$$"more$$$$$$$${"" + text}"} padding"

    $$$$"padding $$$${"more${"" + text}"} padding"
    $$$$"padding $$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more${"" + text}"<!>} padding"
    $$$$"padding $$$${$$"more$${"" + text}"} padding"
    $$$$"padding $$$${$$$$"more$$$${"" + text}"} padding"
    $$$$"padding $$$${$$$$$$$$"more$$$$$$$${"" + text}"} padding"

    $$$$$$$$"padding $$$$$$$${"more${"" + text}"} padding"
    $$$$$$$$"padding $$$$$$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more${"" + text}"<!>} padding"
    $$$$$$$$"padding $$$$$$$${$$"more$${"" + text}"} padding"
    $$$$$$$$"padding $$$$$$$${$$$$"more$$$${"" + text}"} padding"
    $$$$$$$$"padding $$$$$$$${$$$$$$$$"more$$$$$$$${"" + text}"} padding"



    "padding ${"""more$text"""} padding"
    "padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$text"""<!>} padding"
    "padding ${$$"""more$$text"""} padding"
    "padding ${$$$$"""more$$$$text"""} padding"
    "padding ${$$$$$$$$"""more$$$$$$$$text"""} padding"

    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${"""more$text"""} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$text"""<!>} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${$$"""more$$text"""} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${$$$$"""more$$$$text"""} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${$$$$$$$$"""more$$$$$$$$text"""} padding"<!>

    $$"padding $${"""more$text"""} padding"
    $$"padding $${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$text"""<!>} padding"
    $$"padding $${$$"""more$$text"""} padding"
    $$"padding $${$$$$"""more$$$$text"""} padding"
    $$"padding $${$$$$$$$$"""more$$$$$$$$text"""} padding"

    $$$$"padding $$$${"""more$text"""} padding"
    $$$$"padding $$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$text"""<!>} padding"
    $$$$"padding $$$${$$"""more$$text"""} padding"
    $$$$"padding $$$${$$$$"""more$$$$text"""} padding"
    $$$$"padding $$$${$$$$$$$$"""more$$$$$$$$text"""} padding"

    $$$$$$$$"padding $$$$$$$${"""more$text"""} padding"
    $$$$$$$$"padding $$$$$$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$text"""<!>} padding"
    $$$$$$$$"padding $$$$$$$${$$"""more$$text"""} padding"
    $$$$$$$$"padding $$$$$$$${$$$$"""more$$$$text"""} padding"
    $$$$$$$$"padding $$$$$$$${$$$$$$$$"""more$$$$$$$$text"""} padding"


    "padding ${"""more$`text`"""} padding"
    "padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$`text`"""<!>} padding"
    "padding ${$$"""more$$`text`"""} padding"
    "padding ${$$$$"""more$$$$`text`"""} padding"
    "padding ${$$$$$$$$"""more$$$$$$$$`text`"""} padding"

    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${"""more$`text`"""} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$`text`"""<!>} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${$$"""more$$`text`"""} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${$$$$"""more$$$$`text`"""} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${$$$$$$$$"""more$$$$$$$$`text`"""} padding"<!>

    $$"padding $${"""more$`text`"""} padding"
    $$"padding $${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$`text`"""<!>} padding"
    $$"padding $${$$"""more$$`text`"""} padding"
    $$"padding $${$$$$"""more$$$$`text`"""} padding"
    $$"padding $${$$$$$$$$"""more$$$$$$$$`text`"""} padding"

    $$$$"padding $$$${"""more$`text`"""} padding"
    $$$$"padding $$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$`text`"""<!>} padding"
    $$$$"padding $$$${$$"""more$$`text`"""} padding"
    $$$$"padding $$$${$$$$"""more$$$$`text`"""} padding"
    $$$$"padding $$$${$$$$$$$$"""more$$$$$$$$`text`"""} padding"

    $$$$$$$$"padding $$$$$$$${"""more$`text`"""} padding"
    $$$$$$$$"padding $$$$$$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$`text`"""<!>} padding"
    $$$$$$$$"padding $$$$$$$${$$"""more$$`text`"""} padding"
    $$$$$$$$"padding $$$$$$$${$$$$"""more$$$$`text`"""} padding"
    $$$$$$$$"padding $$$$$$$${$$$$$$$$"""more$$$$$$$$`text`"""} padding"


    "padding ${"""more${"" + text}"""} padding"
    "padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more${"" + text}"""<!>} padding"
    "padding ${$$"""more$${"" + text}"""} padding"
    "padding ${$$$$"""more$$$${"" + text}"""} padding"
    "padding ${$$$$$$$$"""more$$$$$$$${"" + text}"""} padding"

    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${"""more${"" + text}"""} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more${"" + text}"""<!>} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${$$"""more$${"" + text}"""} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${$$$$"""more$$$${"" + text}"""} padding"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${$$$$$$$$"""more$$$$$$$${"" + text}"""} padding"<!>

    $$"padding $${"""more${"" + text}"""} padding"
    $$"padding $${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more${"" + text}"""<!>} padding"
    $$"padding $${$$"""more$${"" + text}"""} padding"
    $$"padding $${$$$$"""more$$$${"" + text}"""} padding"
    $$"padding $${$$$$$$$$"""more$$$$$$$${"" + text}"""} padding"

    $$$$"padding $$$${"""more${"" + text}"""} padding"
    $$$$"padding $$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more${"" + text}"""<!>} padding"
    $$$$"padding $$$${$$"""more$${"" + text}"""} padding"
    $$$$"padding $$$${$$$$"""more$$$${"" + text}"""} padding"
    $$$$"padding $$$${$$$$$$$$"""more$$$$$$$${"" + text}"""} padding"

    $$$$$$$$"padding $$$$$$$${"""more${"" + text}"""} padding"
    $$$$$$$$"padding $$$$$$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more${"" + text}"""<!>} padding"
    $$$$$$$$"padding $$$$$$$${$$"""more$${"" + text}"""} padding"
    $$$$$$$$"padding $$$$$$$${$$$$"""more$$$${"" + text}"""} padding"
    $$$$$$$$"padding $$$$$$$${$$$$$$$$"""more$$$$$$$${"" + text}"""} padding"




    """padding ${"more$text"} padding"""
    """padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$text"<!>} padding"""
    """padding ${$$"more$$text"} padding"""
    """padding ${$$$$"more$$$$text"} padding"""
    """padding ${$$$$$$$$"more$$$$$$$$text"} padding"""

    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${"more$text"} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$text"<!>} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${$$"more$$text"} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${$$$$"more$$$$text"} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${$$$$$$$$"more$$$$$$$$text"} padding"""<!>

    $$"""padding $${"more$text"} padding"""
    $$"""padding $${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$text"<!>} padding"""
    $$"""padding $${$$"more$$text"} padding"""
    $$"""padding $${$$$$"more$$$$text"} padding"""
    $$"""padding $${$$$$$$$$"more$$$$$$$$text"} padding"""

    $$$$"""padding $$$${"more$text"} padding"""
    $$$$"""padding $$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$text"<!>} padding"""
    $$$$"""padding $$$${$$"more$$text"} padding"""
    $$$$"""padding $$$${$$$$"more$$$$text"} padding"""
    $$$$"""padding $$$${$$$$$$$$"more$$$$$$$$text"} padding"""

    $$$$$$$$"""padding $$$$$$$${"more$text"} padding"""
    $$$$$$$$"""padding $$$$$$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$text"<!>} padding"""
    $$$$$$$$"""padding $$$$$$$${$$"more$$text"} padding"""
    $$$$$$$$"""padding $$$$$$$${$$$$"more$$$$text"} padding"""
    $$$$$$$$"""padding $$$$$$$${$$$$$$$$"more$$$$$$$$text"} padding"""


    """padding ${"more$`text`"} padding"""
    """padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$`text`"<!>} padding"""
    """padding ${$$"more$$`text`"} padding"""
    """padding ${$$$$"more$$$$`text`"} padding"""
    """padding ${$$$$$$$$"more$$$$$$$$`text`"} padding"""

    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${"more$`text`"} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$`text`"<!>} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${$$"more$$`text`"} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${$$$$"more$$$$`text`"} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${$$$$$$$$"more$$$$$$$$`text`"} padding"""<!>

    $$"""padding $${"more$`text`"} padding"""
    $$"""padding $${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$`text`"<!>} padding"""
    $$"""padding $${$$"more$$`text`"} padding"""
    $$"""padding $${$$$$"more$$$$`text`"} padding"""
    $$"""padding $${$$$$$$$$"more$$$$$$$$`text`"} padding"""

    $$$$"""padding $$$${"more$`text`"} padding"""
    $$$$"""padding $$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$`text`"<!>} padding"""
    $$$$"""padding $$$${$$"more$$`text`"} padding"""
    $$$$"""padding $$$${$$$$"more$$$$`text`"} padding"""
    $$$$"""padding $$$${$$$$$$$$"more$$$$$$$$`text`"} padding"""

    $$$$$$$$"""padding $$$$$$$${"more$`text`"} padding"""
    $$$$$$$$"""padding $$$$$$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more$`text`"<!>} padding"""
    $$$$$$$$"""padding $$$$$$$${$$"more$$`text`"} padding"""
    $$$$$$$$"""padding $$$$$$$${$$$$"more$$$$`text`"} padding"""
    $$$$$$$$"""padding $$$$$$$${$$$$$$$$"more$$$$$$$$`text`"} padding"""


    """padding ${"more${"" + text}"} padding"""
    """padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more${"" + text}"<!>} padding"""
    """padding ${$$"more$${"" + text}"} padding"""
    """padding ${$$$$"more$$$${"" + text}"} padding"""
    """padding ${$$$$$$$$"more$$$$$$$${"" + text}"} padding"""

    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${"more${"" + text}"} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more${"" + text}"<!>} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${$$"more$${"" + text}"} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${$$$$"more$$$${"" + text}"} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${$$$$$$$$"more$$$$$$$${"" + text}"} padding"""<!>

    $$"""padding $${"more${"" + text}"} padding"""
    $$"""padding $${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more${"" + text}"<!>} padding"""
    $$"""padding $${$$"more$${"" + text}"} padding"""
    $$"""padding $${$$$$"more$$$${"" + text}"} padding"""
    $$"""padding $${$$$$$$$$"more$$$$$$$${"" + text}"} padding"""

    $$$$"""padding $$$${"more${"" + text}"} padding"""
    $$$$"""padding $$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more${"" + text}"<!>} padding"""
    $$$$"""padding $$$${$$"more$${"" + text}"} padding"""
    $$$$"""padding $$$${$$$$"more$$$${"" + text}"} padding"""
    $$$$"""padding $$$${$$$$$$$$"more$$$$$$$${"" + text}"} padding"""

    $$$$$$$$"""padding $$$$$$$${"more${"" + text}"} padding"""
    $$$$$$$$"""padding $$$$$$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"more${"" + text}"<!>} padding"""
    $$$$$$$$"""padding $$$$$$$${$$"more$${"" + text}"} padding"""
    $$$$$$$$"""padding $$$$$$$${$$$$"more$$$${"" + text}"} padding"""
    $$$$$$$$"""padding $$$$$$$${$$$$$$$$"more$$$$$$$${"" + text}"} padding"""



    """padding ${"""more$text"""} padding"""
    """padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$text"""<!>} padding"""
    """padding ${$$"""more$$text"""} padding"""
    """padding ${$$$$"""more$$$$text"""} padding"""
    """padding ${$$$$$$$$"""more$$$$$$$$text"""} padding"""

    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${"""more$text"""} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$text"""<!>} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${$$"""more$$text"""} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${$$$$"""more$$$$text"""} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${$$$$$$$$"""more$$$$$$$$text"""} padding"""<!>

    $$"""padding $${"""more$text"""} padding"""
    $$"""padding $${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$text"""<!>} padding"""
    $$"""padding $${$$"""more$$text"""} padding"""
    $$"""padding $${$$$$"""more$$$$text"""} padding"""
    $$"""padding $${$$$$$$$$"""more$$$$$$$$text"""} padding"""

    $$$$"""padding $$$${"""more$text"""} padding"""
    $$$$"""padding $$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$text"""<!>} padding"""
    $$$$"""padding $$$${$$"""more$$text"""} padding"""
    $$$$"""padding $$$${$$$$"""more$$$$text"""} padding"""
    $$$$"""padding $$$${$$$$$$$$"""more$$$$$$$$text"""} padding"""

    $$$$$$$$"""padding $$$$$$$${"""more$text"""} padding"""
    $$$$$$$$"""padding $$$$$$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$text"""<!>} padding"""
    $$$$$$$$"""padding $$$$$$$${$$"""more$$text"""} padding"""
    $$$$$$$$"""padding $$$$$$$${$$$$"""more$$$$text"""} padding"""
    $$$$$$$$"""padding $$$$$$$${$$$$$$$$"""more$$$$$$$$text"""} padding"""


    """padding ${"""more$`text`"""} padding"""
    """padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$`text`"""<!>} padding"""
    """padding ${$$"""more$$`text`"""} padding"""
    """padding ${$$$$"""more$$$$`text`"""} padding"""
    """padding ${$$$$$$$$"""more$$$$$$$$`text`"""} padding"""

    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${"""more$`text`"""} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$`text`"""<!>} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${$$"""more$$`text`"""} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${$$$$"""more$$$$`text`"""} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${$$$$$$$$"""more$$$$$$$$`text`"""} padding"""<!>

    $$"""padding $${"""more$`text`"""} padding"""
    $$"""padding $${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$`text`"""<!>} padding"""
    $$"""padding $${$$"""more$$`text`"""} padding"""
    $$"""padding $${$$$$"""more$$$$`text`"""} padding"""
    $$"""padding $${$$$$$$$$"""more$$$$$$$$`text`"""} padding"""

    $$$$"""padding $$$${"""more$`text`"""} padding"""
    $$$$"""padding $$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$`text`"""<!>} padding"""
    $$$$"""padding $$$${$$"""more$$`text`"""} padding"""
    $$$$"""padding $$$${$$$$"""more$$$$`text`"""} padding"""
    $$$$"""padding $$$${$$$$$$$$"""more$$$$$$$$`text`"""} padding"""

    $$$$$$$$"""padding $$$$$$$${"""more$`text`"""} padding"""
    $$$$$$$$"""padding $$$$$$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more$`text`"""<!>} padding"""
    $$$$$$$$"""padding $$$$$$$${$$"""more$$`text`"""} padding"""
    $$$$$$$$"""padding $$$$$$$${$$$$"""more$$$$`text`"""} padding"""
    $$$$$$$$"""padding $$$$$$$${$$$$$$$$"""more$$$$$$$$`text`"""} padding"""


    """padding ${"""more${"" + text}"""} padding"""
    """padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more${"" + text}"""<!>} padding"""
    """padding ${$$"""more$${"" + text}"""} padding"""
    """padding ${$$$$"""more$$$${"" + text}"""} padding"""
    """padding ${$$$$$$$$"""more$$$$$$$${"" + text}"""} padding"""

    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${"""more${"" + text}"""} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more${"" + text}"""<!>} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${$$"""more$${"" + text}"""} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${$$$$"""more$$$${"" + text}"""} padding"""<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${$$$$$$$$"""more$$$$$$$${"" + text}"""} padding"""<!>

    $$"""padding $${"""more${"" + text}"""} padding"""
    $$"""padding $${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more${"" + text}"""<!>} padding"""
    $$"""padding $${$$"""more$${"" + text}"""} padding"""
    $$"""padding $${$$$$"""more$$$${"" + text}"""} padding"""
    $$"""padding $${$$$$$$$$"""more$$$$$$$${"" + text}"""} padding"""

    $$$$"""padding $$$${"""more${"" + text}"""} padding"""
    $$$$"""padding $$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more${"" + text}"""<!>} padding"""
    $$$$"""padding $$$${$$"""more$${"" + text}"""} padding"""
    $$$$"""padding $$$${$$$$"""more$$$${"" + text}"""} padding"""
    $$$$"""padding $$$${$$$$$$$$"""more$$$$$$$${"" + text}"""} padding"""

    $$$$$$$$"""padding $$$$$$$${"""more${"" + text}"""} padding"""
    $$$$$$$$"""padding $$$$$$$${<!REDUNDANT_INTERPOLATION_PREFIX!>$"""more${"" + text}"""<!>} padding"""
    $$$$$$$$"""padding $$$$$$$${$$"""more$${"" + text}"""} padding"""
    $$$$$$$$"""padding $$$$$$$${$$$$"""more$$$${"" + text}"""} padding"""
    $$$$$$$$"""padding $$$$$$$${$$$$$$$$"""more$$$$$$$${"" + text}"""} padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// string literal kinds: single-line, multi-line
fun multilineInterpolation() {
    "padding ${
        0 + value
    } padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${
        0 + value
    } padding"<!>
    $$"padding $${
    0 + value
    } padding"
    $$$$"padding $$$${
    0 + value
    } padding"
    $$$$$$$$"padding $$$$$$$${
    0 + value
    } padding"

    """padding ${
        0 + value
    } padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${
        0 + value
    } padding"""<!>
    $$"""padding $${
    0 + value
    } padding"""
    $$$$"""padding $$$${
    0 + value
    } padding"""
    $$$$$$$$"""padding $$$$$$$${
    0 + value
    } padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun multilineCommentsInsideStringsWithInterpolation() {
    "padding /* $value */ padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding /* $value */ padding"<!>
    $$"padding /* $$value */ padding"
    $$$$"padding /* $$$$value */ padding"
    $$$$$$$$"padding /* $$$$$$$$value */ padding"

    "padding /* $`value` */ padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding /* $`value` */ padding"<!>
    $$"padding /* $$`value` */ padding"
    $$$$"padding /* $$$$`value` */ padding"
    $$$$$$$$"padding /* $$$$$$$$`value` */ padding"

    "padding /* ${0 + value} */ padding"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding /* ${0 + value} */ padding"<!>
    $$"padding /* $${0 + value} */ padding"
    $$$$"padding /* $$$${0 + value} */ padding"
    $$$$$$$$"padding /* $$$$$$$${0 + value} */ padding"


    """padding /* $value */ padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding /* $value */ padding"""<!>
    $$"""padding /* $$value */ padding"""
    $$$$"""padding /* $$$$value */ padding"""
    $$$$$$$$"""padding /* $$$$$$$$value */ padding"""

    """padding /* $`value` */ padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding /* $`value` */ padding"""<!>
    $$"""padding /* $$`value` */ padding"""
    $$$$"""padding /* $$$$`value` */ padding"""
    $$$$$$$$"""padding /* $$$$$$$$`value` */ padding"""

    """padding /* ${0 + value} */ padding"""
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding /* ${0 + value} */ padding"""<!>
    $$"""padding /* $${0 + value} */ padding"""
    $$$$"""padding /* $$$${0 + value} */ padding"""
    $$$$$$$$"""padding /* $$$$$$$${0 + value} */ padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun stringsWithInterpolationInsideSingleLineComments() {
    // "padding $value padding"
    // $"padding $value padding"
    // $$"padding $$value padding"
    // $$$$"padding $$$$value padding"
    // $$$$$$$$"padding $$$$$$$$value padding"

    // "padding $`value` padding"
    // $"padding $`value` padding"
    // $$"padding $$`value` padding"
    // $$$$"padding $$$$`value` padding"
    // $$$$$$$$"padding $$$$$$$$`value` padding"

    // "padding ${0 + value} padding"
    // $"padding ${0 + value} padding"
    // $$"padding $${0 + value} padding"
    // $$$$"padding $$$${0 + value} padding"
    // $$$$$$$$"padding $$$$$$$${0 + value} padding"


    // """padding $value padding"""
    // $"""padding $value padding"""
    // $$"""padding $$value padding"""
    // $$$$"""padding $$$$value padding"""
    // $$$$$$$$"""padding $$$$$$$$value padding"""

    // """padding $`value` padding"""
    // $"""padding $`value` padding"""
    // $$"""padding $$`value` padding"""
    // $$$$"""padding $$$$`value` padding"""
    // $$$$$$$$"""padding $$$$$$$$`value` padding"""

    // """padding ${0 + value} padding"""
    // $"""padding ${0 + value} padding"""
    // $$"""padding $${0 + value} padding"""
    // $$$$"""padding $$$${0 + value} padding"""
    // $$$$$$$$"""padding $$$$$$$${0 + value} padding"""
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun stringsWithInterpolationInsideMultiLineComments() {
    /* "padding $value padding" */
    /* $"padding $value padding" */
    /* $$"padding $$value padding" */
    /* $$$$"padding $$$$value padding" */
    /* $$$$$$$$"padding $$$$$$$$value padding" */

    /* "padding $`value` padding" */
    /* $"padding $`value` padding" */
    /* $$"padding $$`value` padding" */
    /* $$$$"padding $$$$`value` padding" */
    /* $$$$$$$$"padding $$$$$$$$`value` padding" */

    /* "padding ${0 + value} padding" */
    /* $"padding ${0 + value} padding" */
    /* $$"padding $${0 + value} padding" */
    /* $$$$"padding $$$${0 + value} padding" */
    /* $$$$$$$$"padding $$$$$$$${0 + value} padding" */


    /* """padding $value padding""" */
    /* $"""padding $value padding""" */
    /* $$"""padding $$value padding""" */
    /* $$$$"""padding $$$$value padding""" */
    /* $$$$$$$$"""padding $$$$$$$$value padding""" */

    /* """padding $`value` padding""" */
    /* $"""padding $`value` padding""" */
    /* $$"""padding $$`value` padding""" */
    /* $$$$"""padding $$$$`value` padding""" */
    /* $$$$$$$$"""padding $$$$$$$$`value` padding""" */

    /* """padding ${0 + value} padding""" */
    /* $"""padding ${0 + value} padding""" */
    /* $$"""padding $${0 + value} padding""" */
    /* $$$$"""padding $$$${0 + value} padding""" */
    /* $$$$$$$$"""padding $$$$$$$${0 + value} padding""" */
}

const val value = 42
const val text = "text"
const val `$` = "$"

const val compileTimeConstant = 42

@Repeatable annotation class Annotation(val value: String)

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line

@Annotation("padding $compileTimeConstant padding")
@Annotation(<!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $compileTimeConstant padding"<!>)
@Annotation($$"padding $$compileTimeConstant padding")
@Annotation($$$$"padding $$$$compileTimeConstant padding")
@Annotation($$$$$$$$"padding $$$$$$$$compileTimeConstant padding")

@Annotation("padding $`compileTimeConstant` padding")
@Annotation(<!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $`compileTimeConstant` padding"<!>)
@Annotation($$"padding $$`compileTimeConstant` padding")
@Annotation($$$$"padding $$$$`compileTimeConstant` padding")
@Annotation($$$$$$$$"padding $$$$$$$$`compileTimeConstant` padding")

@Annotation("padding ${0 + compileTimeConstant} padding")
@Annotation(<!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${0 + compileTimeConstant} padding"<!>)
@Annotation($$"padding $${0 + compileTimeConstant} padding")
@Annotation($$$$"padding $$$${0 + compileTimeConstant} padding")
@Annotation($$$$$$$$"padding $$$$$$$${0 + compileTimeConstant} padding")


@Annotation("""padding $compileTimeConstant padding""")
@Annotation(<!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $compileTimeConstant padding"""<!>)
@Annotation($$"""padding $$compileTimeConstant padding""")
@Annotation($$$$"""padding $$$$compileTimeConstant padding""")
@Annotation($$$$$$$$"""padding $$$$$$$$compileTimeConstant padding""")

@Annotation("""padding $`compileTimeConstant` padding""")
@Annotation(<!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $`compileTimeConstant` padding"""<!>)
@Annotation($$"""padding $$`compileTimeConstant` padding""")
@Annotation($$$$"""padding $$$$`compileTimeConstant` padding""")
@Annotation($$$$$$$$"""padding $$$$$$$$`compileTimeConstant` padding""")

@Annotation("""padding ${0 + compileTimeConstant} padding""")
@Annotation(<!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${0 + compileTimeConstant} padding"""<!>)
@Annotation($$"""padding $${0 + compileTimeConstant} padding""")
@Annotation($$$$"""padding $$$${0 + compileTimeConstant} padding""")
@Annotation($$$$$$$$"""padding $$$$$$$${0 + compileTimeConstant} padding""")

fun stringsWithInterpolationAsValidAnnotationArguments() {}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line

const val stringWithInterpolationAsValidConstantInitializer01 = "padding $compileTimeConstant padding"
const val stringWithInterpolationAsValidConstantInitializer02 = <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $compileTimeConstant padding"<!>
const val stringWithInterpolationAsValidConstantInitializer03 = $$"padding $$compileTimeConstant padding"
const val stringWithInterpolationAsValidConstantInitializer04 = $$$$"padding $$$$compileTimeConstant padding"
const val stringWithInterpolationAsValidConstantInitializer05 = $$$$$$$$"padding $$$$$$$$compileTimeConstant padding"

const val stringWithInterpolationAsValidConstantInitializer06 = "padding $`compileTimeConstant` padding"
const val stringWithInterpolationAsValidConstantInitializer07 = <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding $`compileTimeConstant` padding"<!>
const val stringWithInterpolationAsValidConstantInitializer08 = $$"padding $$`compileTimeConstant` padding"
const val stringWithInterpolationAsValidConstantInitializer09 = $$$$"padding $$$$`compileTimeConstant` padding"
const val stringWithInterpolationAsValidConstantInitializer10 = $$$$$$$$"padding $$$$$$$$`compileTimeConstant` padding"

const val stringWithInterpolationAsValidConstantInitializer11 = "padding ${0 + compileTimeConstant} padding"
const val stringWithInterpolationAsValidConstantInitializer12 = <!REDUNDANT_INTERPOLATION_PREFIX!>$"padding ${0 + compileTimeConstant} padding"<!>
const val stringWithInterpolationAsValidConstantInitializer13 = $$"padding $${0 + compileTimeConstant} padding"
const val stringWithInterpolationAsValidConstantInitializer14 = $$$$"padding $$$${0 + compileTimeConstant} padding"
const val stringWithInterpolationAsValidConstantInitializer15 = $$$$$$$$"padding $$$$$$$${0 + compileTimeConstant} padding"


const val stringWithInterpolationAsValidConstantInitializer16 = """padding $compileTimeConstant padding"""
const val stringWithInterpolationAsValidConstantInitializer17 = <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $compileTimeConstant padding"""<!>
const val stringWithInterpolationAsValidConstantInitializer18 = $$"""padding $$compileTimeConstant padding"""
const val stringWithInterpolationAsValidConstantInitializer19 = $$$$"""padding $$$$compileTimeConstant padding"""
const val stringWithInterpolationAsValidConstantInitializer20 = $$$$$$$$"""padding $$$$$$$$compileTimeConstant padding"""

const val stringWithInterpolationAsValidConstantInitializer21 = """padding $`compileTimeConstant` padding"""
const val stringWithInterpolationAsValidConstantInitializer22 = <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding $`compileTimeConstant` padding"""<!>
const val stringWithInterpolationAsValidConstantInitializer23 = $$"""padding $$`compileTimeConstant` padding"""
const val stringWithInterpolationAsValidConstantInitializer24 = $$$$"""padding $$$$`compileTimeConstant` padding"""
const val stringWithInterpolationAsValidConstantInitializer25 = $$$$$$$$"""padding $$$$$$$$`compileTimeConstant` padding"""

const val stringWithInterpolationAsValidConstantInitializer26 = """padding ${0 + compileTimeConstant} padding"""
const val stringWithInterpolationAsValidConstantInitializer27 = <!REDUNDANT_INTERPOLATION_PREFIX!>$"""padding ${0 + compileTimeConstant} padding"""<!>
const val stringWithInterpolationAsValidConstantInitializer28 = $$"""padding $${0 + compileTimeConstant} padding"""
const val stringWithInterpolationAsValidConstantInitializer29 = $$$$"""padding $$$${0 + compileTimeConstant} padding"""
const val stringWithInterpolationAsValidConstantInitializer30 = $$$$$$$$"""padding $$$$$$$${0 + compileTimeConstant} padding"""
