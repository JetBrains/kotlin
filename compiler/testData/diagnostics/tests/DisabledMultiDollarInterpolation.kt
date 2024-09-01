// LANGUAGE: -MultiDollarInterpolation
// WITH_STDLIB

// FIR_IDENTICAL

// FIR_DUMP
// REASON: KT-68971

// ISSUE: KT-69062
// ISSUE: KT-68957

// interpolation prefix length: 0, 1, 2, 4, 8
// string literal kinds: single-line, multi-line
fun emptyStrings() {
    ""
    <!UNSUPPORTED_FEATURE!>$""<!>
    <!UNSUPPORTED_FEATURE!>$$""<!>
    <!UNSUPPORTED_FEATURE!>$$$$""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$""<!>

    """"""
    <!UNSUPPORTED_FEATURE!>$""""""<!>
    <!UNSUPPORTED_FEATURE!>$$""""""<!>
    <!UNSUPPORTED_FEATURE!>$$$$""""""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$""""""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// string literal kinds: single-line, multi-line
fun stringsWithoutInterpolation() {
    "padding"
    <!UNSUPPORTED_FEATURE!>$"padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding"<!>

    """padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun dollarInStrings() {
    "padding $ padding"
    <!UNSUPPORTED_FEATURE!>$"padding $ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $ padding"<!>

    """padding $ padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $ padding"""<!>


    "padding $ padding $text"
    <!UNSUPPORTED_FEATURE!>$"padding $ padding $text"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $ padding $$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $ padding $$$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $ padding $$$$$$$$text"<!>

    """padding $ padding $text"""
    <!UNSUPPORTED_FEATURE!>$"""padding $ padding $text"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $ padding $$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $ padding $$$$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $ padding $$$$$$$$text"""<!>
}

// interpolation prefix length: 2, 4, 8
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun dollarsInStringsA() {
    <!UNSUPPORTED_FEATURE!>$$"padding $$ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$ padding"<!>

    <!UNSUPPORTED_FEATURE!>$$"""padding $$ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$ padding"""<!>


    <!UNSUPPORTED_FEATURE!>$$"padding $$ padding $$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$ padding $$$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$ padding $$$$$$$$text"<!>

    <!UNSUPPORTED_FEATURE!>$$"""padding $$ padding $$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$ padding $$$$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$ padding $$$$$$$$text"""<!>
}

// interpolation prefix length: 2, 4, 8
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun dollarsInStringsB() {
    <!UNSUPPORTED_FEATURE!>$$"padding $ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$ padding"<!>

    <!UNSUPPORTED_FEATURE!>$$"""padding $ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$ padding"""<!>


    <!UNSUPPORTED_FEATURE!>$$"padding $ padding $$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$ padding $$$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$ padding $$$$$$$$text"<!>

    <!UNSUPPORTED_FEATURE!>$$"""padding $ padding $$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$ padding $$$$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$ padding $$$$$$$$text"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun dollarsInStringsC() {
    "padding $$ padding"
    <!UNSUPPORTED_FEATURE!>$"padding $$ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$$ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$$ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$$ padding"<!>

    """padding $$ padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $$ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$$ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$$ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$$ padding"""<!>


    "padding $$ padding $text"
    <!UNSUPPORTED_FEATURE!>$"padding $$ padding $text"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$$ padding $$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$$ padding $$$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$$ padding $$$$$$$$text"<!>

    """padding $$ padding $text"""
    <!UNSUPPORTED_FEATURE!>$"""padding $$ padding $text"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$$ padding $$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$$ padding $$$$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$$ padding $$$$$$$$text"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun dollarsWithoutInterpolationOfSimpleIdentifierA() {
    "padding $ value padding"
    <!UNSUPPORTED_FEATURE!>$"padding $ value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$ value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$ value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$ value padding"<!>

    """padding $ value padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $ value padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$ value padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$ value padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$ value padding"""<!>


    "padding $ value padding $text"
    <!UNSUPPORTED_FEATURE!>$"padding $ value padding $text"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$ value padding $$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$ value padding $$$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$ value padding $$$$$$$$text"<!>

    """padding $ value padding $text"""
    <!UNSUPPORTED_FEATURE!>$"""padding $ value padding $text"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$ value padding $$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$ value padding $$$$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$ value padding $$$$$$$$text"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun dollarsWithoutInterpolationOfSimpleIdentifierB() {
    "padding $-value padding"
    <!UNSUPPORTED_FEATURE!>$"padding $-value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$-value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$-value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$-value padding"<!>

    """padding $-value padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $-value padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$-value padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$-value padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$-value padding"""<!>


    "padding $-value padding $text"
    <!UNSUPPORTED_FEATURE!>$"padding $-value padding $text"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$-value padding $$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$-value padding $$$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$-value padding $$$$$$$$text"<!>

    """padding $-value padding $text"""
    <!UNSUPPORTED_FEATURE!>$"""padding $-value padding $text"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$-value padding $$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$-value padding $$$$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$-value padding $$$$$$$$text"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun dollarsWithoutInterpolationOfIdentifierInBackticks() {
    "padding $`` padding"
    <!UNSUPPORTED_FEATURE!>$"padding $`` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$`` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$`` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$`` padding"<!>

    """padding $`` padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $`` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$`` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$`` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$`` padding"""<!>


    "padding $`` padding $text"
    <!UNSUPPORTED_FEATURE!>$"padding $`` padding $text"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$`` padding $$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$`` padding $$$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$`` padding $$$$$$$$text"<!>

    """padding $`` padding $text"""
    <!UNSUPPORTED_FEATURE!>$"""padding $`` padding $text"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$`` padding $$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$`` padding $$$$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$`` padding $$$$$$$$text"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun dollarsWithoutInterpolationOfArbitraryExpression() {
    "padding $ {0 + value} padding"
    <!UNSUPPORTED_FEATURE!>$"padding $ {0 + value} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$ {0 + value} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$ {0 + value} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$ {0 + value} padding"<!>

    """padding $ {0 + value} padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $ {0 + value} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$ {0 + value} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$ {0 + value} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$ {0 + value} padding"""<!>


    "padding $ {0 + value} padding $text"
    <!UNSUPPORTED_FEATURE!>$"padding $ {0 + value} padding $text"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$ {0 + value} padding $$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$ {0 + value} padding $$$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$ {0 + value} padding $$$$$$$$text"<!>

    """padding $ {0 + value} padding $text"""
    <!UNSUPPORTED_FEATURE!>$"""padding $ {0 + value} padding $text"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$ {0 + value} padding $$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$ {0 + value} padding $$$$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$ {0 + value} padding $$$$$$$$text"""<!>
}

// interpolation prefix length: 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun insufficientDollarForInterpolation() {
    <!UNSUPPORTED_FEATURE!>$$"padding $value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $value padding"<!>

    <!UNSUPPORTED_FEATURE!>$$"padding $`value` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $`value` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $`value` padding"<!>

    <!UNSUPPORTED_FEATURE!>$$"padding ${0 + value} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding ${0 + value} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding ${0 + value} padding"<!>


    <!UNSUPPORTED_FEATURE!>$$"""padding $value padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $value padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $value padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$"""padding $`value` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $`value` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $`value` padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$"""padding ${0 + value} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding ${0 + value} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding ${0 + value} padding"""<!>



    <!UNSUPPORTED_FEATURE!>$$"padding $value padding $$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $value padding $$$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $value padding $$$$$$$$text"<!>

    <!UNSUPPORTED_FEATURE!>$$"padding $`value` padding $$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $`value` padding $$$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $`value` padding $$$$$$$$text"<!>

    <!UNSUPPORTED_FEATURE!>$$"padding ${0 + value} padding $$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding ${0 + value} padding $$$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding ${0 + value} padding $$$$$$$$text"<!>


    <!UNSUPPORTED_FEATURE!>$$"""padding $value padding $$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $value padding $$$$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $value padding $$$$$$$$text"""<!>

    <!UNSUPPORTED_FEATURE!>$$"""padding $`value` padding $$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $`value` padding $$$$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $`value` padding $$$$$$$$text"""<!>

    <!UNSUPPORTED_FEATURE!>$$"""padding ${0 + value} padding $$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding ${0 + value} padding $$$$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding ${0 + value} padding $$$$$$$$text"""<!>
}

// interpolation prefix length: 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
// interpolation as padding: no, yes
fun insufficientDollarsForInterpolation() {
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$value padding"<!>

    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$`value` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$`value` padding"<!>

    <!UNSUPPORTED_FEATURE!>$$$$"padding $$${0 + value} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$${0 + value} padding"<!>


    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$value padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$value padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$`value` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$`value` padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$${0 + value} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$${0 + value} padding"""<!>



    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$value padding $$$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$value padding $$$$$$$$text"<!>

    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$`value` padding $$$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$`value` padding $$$$$$$$text"<!>

    <!UNSUPPORTED_FEATURE!>$$$$"padding $$${0 + value} padding $$$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$${0 + value} padding $$$$$$$$text"<!>


    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$value padding $$$$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$value padding $$$$$$$$text"""<!>

    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$`value` padding $$$$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$`value` padding $$$$$$$$text"""<!>

    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$${0 + value} padding $$$$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$${0 + value} padding $$$$$$$$text"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line
// interpolation as padding: no, yes
fun escapedDollarInInterpolationPrefix() {
    "padding \$value padding"
    <!UNSUPPORTED_FEATURE!>$"padding \$value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding \$$value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding \$$$$value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding \$$$$$$$$value padding"<!>

    "padding \$`value` padding"
    <!UNSUPPORTED_FEATURE!>$"padding \$`value` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding \$$`value` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding \$$$$`value` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding \$$$$$$$$`value` padding"<!>

    "padding \${0 + value} padding"
    <!UNSUPPORTED_FEATURE!>$"padding \${0 + value} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding \$${0 + value} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding \$$$${0 + value} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding \$$$$$$$${0 + value} padding"<!>


    "padding \$value padding $text"
    <!UNSUPPORTED_FEATURE!>$"padding \$value padding $text"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding \$$value padding $$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding \$$$$value padding $$$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding \$$$$$$$$value padding $$$$$$$$text"<!>

    "padding \$`value` padding $text"
    <!UNSUPPORTED_FEATURE!>$"padding \$`value` padding $text"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding \$$`value` padding $$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding \$$$$`value` padding $$$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding \$$$$$$$$`value` padding $$$$$$$$text"<!>

    "padding \${0 + value} padding $text"
    <!UNSUPPORTED_FEATURE!>$"padding \${0 + value} padding $text"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding \$${0 + value} padding $$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding \$$$${0 + value} padding $$$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding \$$$$$$$${0 + value} padding $$$$$$$$text"<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun stringsWithRedundantInterpolation() {
    "$text"
    <!UNSUPPORTED_FEATURE!>$"$text"<!>
    <!UNSUPPORTED_FEATURE!>$$"$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"$$$$text"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"$$$$$$$$text"<!>

    "$`text`"
    <!UNSUPPORTED_FEATURE!>$"$`text`"<!>
    <!UNSUPPORTED_FEATURE!>$$"$$`text`"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"$$$$`text`"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"$$$$$$$$`text`"<!>

    "${"" + text}"
    <!UNSUPPORTED_FEATURE!>$"${"" + text}"<!>
    <!UNSUPPORTED_FEATURE!>$$"$${"" + text}"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"$$$${"" + text}"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"$$$$$$$${"" + text}"<!>


    """$text"""
    <!UNSUPPORTED_FEATURE!>$"""$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""$$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""$$$$text"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""$$$$$$$$text"""<!>

    """$`text`"""
    <!UNSUPPORTED_FEATURE!>$"""$`text`"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""$$`text`"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""$$$$`text`"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""$$$$$$$$`text`"""<!>

    """${"" + text}"""
    <!UNSUPPORTED_FEATURE!>$"""${"" + text}"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""$${"" + text}"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""$$$${"" + text}"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""$$$$$$$${"" + text}"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun stringsWithInterpolationA() {
    "padding $text padding"
    <!UNSUPPORTED_FEATURE!>$"padding $text padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$text padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$text padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$text padding"<!>

    "padding $`text` padding"
    <!UNSUPPORTED_FEATURE!>$"padding $`text` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$`text` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$`text` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$`text` padding"<!>

    "padding ${"" + text} padding"
    <!UNSUPPORTED_FEATURE!>$"padding ${"" + text} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${"" + text} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${"" + text} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${"" + text} padding"<!>


    """padding $text padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $text padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$text padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$text padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$text padding"""<!>

    """padding $`text` padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $`text` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$`text` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$`text` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$`text` padding"""<!>

    """padding ${"" + text} padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding ${"" + text} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${"" + text} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${"" + text} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${"" + text} padding"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun stringsWithInterpolationB() {
    "$value"
    <!UNSUPPORTED_FEATURE!>$"$value"<!>
    <!UNSUPPORTED_FEATURE!>$$"$$value"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"$$$$value"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"$$$$$$$$value"<!>

    "$`value`"
    <!UNSUPPORTED_FEATURE!>$"$`value`"<!>
    <!UNSUPPORTED_FEATURE!>$$"$$`value`"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"$$$$`value`"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"$$$$$$$$`value`"<!>

    "${0 + value}"
    <!UNSUPPORTED_FEATURE!>$"${0 + value}"<!>
    <!UNSUPPORTED_FEATURE!>$$"$${0 + value}"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"$$$${0 + value}"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"$$$$$$$${0 + value}"<!>


    """$value"""
    <!UNSUPPORTED_FEATURE!>$"""$value"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""$$value"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""$$$$value"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""$$$$$$$$value"""<!>

    """$`value`"""
    <!UNSUPPORTED_FEATURE!>$"""$`value`"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""$$`value`"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""$$$$`value`"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""$$$$$$$$`value`"""<!>

    """${0 + value}"""
    <!UNSUPPORTED_FEATURE!>$"""${0 + value}"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""$${0 + value}"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""$$$${0 + value}"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""$$$$$$$${0 + value}"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfDollarSymbolA() {
    "padding ${'$'}value"
    <!UNSUPPORTED_FEATURE!>$"padding ${'$'}value"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${'$'}value"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${'$'}value"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${'$'}value"<!>

    """padding ${'$'}value"""
    <!UNSUPPORTED_FEATURE!>$"""padding ${'$'}value"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${'$'}value"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${'$'}value"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${'$'}value"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of arbitrary expression
// string literal kinds: single-line, multi-line
fun interpolationOfDollarSymbolB() {
    "padding ${'$'}$value padding"
    <!UNSUPPORTED_FEATURE!>$"padding ${'$'}$value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${'$'}$$value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${'$'}$$$$value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${'$'}$$$$$$$$value padding"<!>

    """padding ${'$'}$value padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding ${'$'}$value padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${'$'}$$value padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${'$'}$$$$value padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${'$'}$$$$$$$$value padding"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of identifier in backticks
// string literal kinds: single-line, multi-line
fun interpolationOfDollarClassifierA() {
    "padding $`$`value"
    <!UNSUPPORTED_FEATURE!>$"padding $`$`value"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$`$`value"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$`$`value"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$`$`value"<!>

    """padding $`$`value"""
    <!UNSUPPORTED_FEATURE!>$"""padding $`$`value"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$`$`value"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$`$`value"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$`$`value"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of identifier in backticks
// string literal kinds: single-line, multi-line
fun interpolationOfDollarClassifierB() {
    "padding $`$`$value padding"
    <!UNSUPPORTED_FEATURE!>$"padding $`$`$value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$`$`$$value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$`$`$$$$value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$`$`$$$$$$$$value padding"<!>

    """padding $`$`$value padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $`$`$value padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$`$`$$value padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$`$`$$$$value padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$`$`$$$$$$$$value padding"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun excessiveDollarsForInterpolation() {
    "padding $$value padding"
    <!UNSUPPORTED_FEATURE!>$"padding $$value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$$value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$$value padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$$value padding"<!>

    "padding $$`value` padding"
    <!UNSUPPORTED_FEATURE!>$"padding $$`value` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$$`value` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$$`value` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$$`value` padding"<!>

    "padding $${0 + value} padding"
    <!UNSUPPORTED_FEATURE!>$"padding $${0 + value} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$${0 + value} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$${0 + value} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$${0 + value} padding"<!>


    """padding $$value padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $$value padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$$value padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$$value padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$$value padding"""<!>

    """padding $$`value` padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $$`value` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$$`value` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$$`value` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$$`value` padding"""<!>

    """padding $${0 + value} padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $${0 + value} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$${0 + value} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$${0 + value} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$${0 + value} padding"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds A: of simple identifier, of identifier in backticks, of arbitrary expression
// interpolation kinds B: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun sequentialInterpolation() {
    "padding $value$text padding"
    <!UNSUPPORTED_FEATURE!>$"padding $value$text padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$value$$text padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$value$$$$text padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$value$$$$$$$$text padding"<!>

    "padding $`value`$text padding"
    <!UNSUPPORTED_FEATURE!>$"padding $`value`$text padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$`value`$$text padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$`value`$$$$text padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$`value`$$$$$$$$text padding"<!>

    "padding ${0 + value}$text padding"
    <!UNSUPPORTED_FEATURE!>$"padding ${0 + value}$text padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${0 + value}$$text padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${0 + value}$$$$text padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${0 + value}$$$$$$$$text padding"<!>


    "padding $value$`text` padding"
    <!UNSUPPORTED_FEATURE!>$"padding $value$`text` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$value$$`text` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$value$$$$`text` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$value$$$$$$$$`text` padding"<!>

    "padding $`value`$`text` padding"
    <!UNSUPPORTED_FEATURE!>$"padding $`value`$`text` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$`value`$$`text` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$`value`$$$$`text` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$`value`$$$$$$$$`text` padding"<!>

    "padding ${0 + value}$`text` padding"
    <!UNSUPPORTED_FEATURE!>$"padding ${0 + value}$`text` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${0 + value}$$`text` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${0 + value}$$$$`text` padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${0 + value}$$$$$$$$`text` padding"<!>


    "padding $value${"" + text} padding"
    <!UNSUPPORTED_FEATURE!>$"padding $value${"" + text} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$value$${"" + text} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$value$$$${"" + text} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$value$$$$$$$${"" + text} padding"<!>

    "padding $`value`${"" + text} padding"
    <!UNSUPPORTED_FEATURE!>$"padding $`value`${"" + text} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $$`value`$${"" + text} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$`value`$$$${"" + text} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$`value`$$$$$$$${"" + text} padding"<!>

    "padding ${0 + value}${"" + text} padding"
    <!UNSUPPORTED_FEATURE!>$"padding ${0 + value}${"" + text} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${0 + value}$${"" + text} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${0 + value}$$$${"" + text} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${0 + value}$$$$$$$${"" + text} padding"<!>



    """padding $value$text padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $value$text padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$value$$text padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$value$$$$text padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$value$$$$$$$$text padding"""<!>

    """padding $`value`$text padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $`value`$text padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$`value`$$text padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$`value`$$$$text padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$`value`$$$$$$$$text padding"""<!>

    """padding ${0 + value}$text padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding ${0 + value}$text padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${0 + value}$$text padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${0 + value}$$$$text padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${0 + value}$$$$$$$$text padding"""<!>


    """padding $value$`text` padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $value$`text` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$value$$`text` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$value$$$$`text` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$value$$$$$$$$`text` padding"""<!>

    """padding $`value`$`text` padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $`value`$`text` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$`value`$$`text` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$`value`$$$$`text` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$`value`$$$$$$$$`text` padding"""<!>

    """padding ${0 + value}$`text` padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding ${0 + value}$`text` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${0 + value}$$`text` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${0 + value}$$$$`text` padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${0 + value}$$$$$$$$`text` padding"""<!>


    """padding $value${"" + text} padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $value${"" + text} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$value$${"" + text} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$value$$$${"" + text} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$value$$$$$$$${"" + text} padding"""<!>

    """padding $`value`${"" + text} padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding $`value`${"" + text} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $$`value`$${"" + text} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$`value`$$$${"" + text} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$`value`$$$$$$$${"" + text} padding"""<!>

    """padding ${0 + value}${"" + text} padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding ${0 + value}${"" + text} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${0 + value}$${"" + text} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${0 + value}$$$${"" + text} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${0 + value}$$$$$$$${"" + text} padding"""<!>
}

// inner interpolation prefix length: 0, 1, 2, 4, 8
// outer interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// inner string literal kinds: single-line, multi-line
// outer string literal kinds: single-line, multi-line
fun nestedInterpolation() {
    "padding ${"more$text"} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$"more$text"<!>} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$$"more$$text"<!>} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$text"<!>} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$text"<!>} padding"

    <!UNSUPPORTED_FEATURE!>$"padding ${"more$text"} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$"more$text"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$$"more$$text"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$text"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$text"<!>} padding"<!>

    <!UNSUPPORTED_FEATURE!>$$"padding $${"more$text"} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$"more$text"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$$"more$$text"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$text"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$text"<!>} padding"<!>

    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${"more$text"} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$"more$text"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$$"more$$text"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$text"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$text"<!>} padding"<!>

    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${"more$text"} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$"more$text"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$"more$$text"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$text"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$text"<!>} padding"<!>


    "padding ${"more$`text`"} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$"more$`text`"<!>} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$$"more$$`text`"<!>} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$`text`"<!>} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$`text`"<!>} padding"

    <!UNSUPPORTED_FEATURE!>$"padding ${"more$`text`"} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$"more$`text`"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$$"more$$`text`"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$`text`"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$`text`"<!>} padding"<!>

    <!UNSUPPORTED_FEATURE!>$$"padding $${"more$`text`"} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$"more$`text`"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$$"more$$`text`"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$`text`"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$`text`"<!>} padding"<!>

    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${"more$`text`"} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$"more$`text`"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$$"more$$`text`"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$`text`"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$`text`"<!>} padding"<!>

    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${"more$`text`"} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$"more$`text`"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$"more$$`text`"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$`text`"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$`text`"<!>} padding"<!>


    "padding ${"more${"" + text}"} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$"more${"" + text}"<!>} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$$"more$${"" + text}"<!>} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$$$$"more$$$${"" + text}"<!>} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$${"" + text}"<!>} padding"

    <!UNSUPPORTED_FEATURE!>$"padding ${"more${"" + text}"} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$"more${"" + text}"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$$"more$${"" + text}"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$$$$"more$$$${"" + text}"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$${"" + text}"<!>} padding"<!>

    <!UNSUPPORTED_FEATURE!>$$"padding $${"more${"" + text}"} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$"more${"" + text}"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$$"more$${"" + text}"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$$$$"more$$$${"" + text}"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$${"" + text}"<!>} padding"<!>

    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${"more${"" + text}"} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$"more${"" + text}"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$$"more$${"" + text}"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$$$$"more$$$${"" + text}"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$${"" + text}"<!>} padding"<!>

    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${"more${"" + text}"} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$"more${"" + text}"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$"more$${"" + text}"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$"more$$$${"" + text}"<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$${"" + text}"<!>} padding"<!>



    "padding ${"""more$text"""} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$"""more$text"""<!>} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$$"""more$$text"""<!>} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$text"""<!>} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$text"""<!>} padding"

    <!UNSUPPORTED_FEATURE!>$"padding ${"""more$text"""} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$"""more$text"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$$"""more$$text"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$text"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$text"""<!>} padding"<!>

    <!UNSUPPORTED_FEATURE!>$$"padding $${"""more$text"""} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$"""more$text"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$$"""more$$text"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$text"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$text"""<!>} padding"<!>

    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${"""more$text"""} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$"""more$text"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$$"""more$$text"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$text"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$text"""<!>} padding"<!>

    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${"""more$text"""} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$"""more$text"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$"""more$$text"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$text"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$text"""<!>} padding"<!>


    "padding ${"""more$`text`"""} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$"""more$`text`"""<!>} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$$"""more$$`text`"""<!>} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$`text`"""<!>} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$`text`"""<!>} padding"

    <!UNSUPPORTED_FEATURE!>$"padding ${"""more$`text`"""} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$"""more$`text`"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$$"""more$$`text`"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$`text`"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$`text`"""<!>} padding"<!>

    <!UNSUPPORTED_FEATURE!>$$"padding $${"""more$`text`"""} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$"""more$`text`"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$$"""more$$`text`"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$`text`"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$`text`"""<!>} padding"<!>

    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${"""more$`text`"""} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$"""more$`text`"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$$"""more$$`text`"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$`text`"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$`text`"""<!>} padding"<!>

    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${"""more$`text`"""} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$"""more$`text`"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$"""more$$`text`"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$`text`"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$`text`"""<!>} padding"<!>


    "padding ${"""more${"" + text}"""} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$"""more${"" + text}"""<!>} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$$"""more$${"" + text}"""<!>} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$${"" + text}"""<!>} padding"
    "padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$${"" + text}"""<!>} padding"

    <!UNSUPPORTED_FEATURE!>$"padding ${"""more${"" + text}"""} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$"""more${"" + text}"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$$"""more$${"" + text}"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$${"" + text}"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$"padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$${"" + text}"""<!>} padding"<!>

    <!UNSUPPORTED_FEATURE!>$$"padding $${"""more${"" + text}"""} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$"""more${"" + text}"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$$"""more$${"" + text}"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$${"" + text}"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$${"" + text}"""<!>} padding"<!>

    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${"""more${"" + text}"""} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$"""more${"" + text}"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$$"""more$${"" + text}"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$${"" + text}"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$${"" + text}"""<!>} padding"<!>

    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${"""more${"" + text}"""} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$"""more${"" + text}"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$"""more$${"" + text}"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$${"" + text}"""<!>} padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$${"" + text}"""<!>} padding"<!>




    """padding ${"more$text"} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$"more$text"<!>} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$$"more$$text"<!>} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$text"<!>} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$text"<!>} padding"""

    <!UNSUPPORTED_FEATURE!>$"""padding ${"more$text"} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$"more$text"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$$"more$$text"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$text"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$text"<!>} padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$"""padding $${"more$text"} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$"more$text"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$$"more$$text"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$text"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$text"<!>} padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${"more$text"} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$"more$text"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$$"more$$text"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$text"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$text"<!>} padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${"more$text"} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$"more$text"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$"more$$text"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$text"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$text"<!>} padding"""<!>


    """padding ${"more$`text`"} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$"more$`text`"<!>} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$$"more$$`text`"<!>} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$`text`"<!>} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$`text`"<!>} padding"""

    <!UNSUPPORTED_FEATURE!>$"""padding ${"more$`text`"} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$"more$`text`"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$$"more$$`text`"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$`text`"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$`text`"<!>} padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$"""padding $${"more$`text`"} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$"more$`text`"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$$"more$$`text`"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$`text`"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$`text`"<!>} padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${"more$`text`"} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$"more$`text`"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$$"more$$`text`"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$`text`"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$`text`"<!>} padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${"more$`text`"} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$"more$`text`"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$"more$$`text`"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$"more$$$$`text`"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$$`text`"<!>} padding"""<!>


    """padding ${"more${"" + text}"} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$"more${"" + text}"<!>} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$$"more$${"" + text}"<!>} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$$$$"more$$$${"" + text}"<!>} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$${"" + text}"<!>} padding"""

    <!UNSUPPORTED_FEATURE!>$"""padding ${"more${"" + text}"} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$"more${"" + text}"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$$"more$${"" + text}"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$$$$"more$$$${"" + text}"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$${"" + text}"<!>} padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$"""padding $${"more${"" + text}"} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$"more${"" + text}"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$$"more$${"" + text}"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$$$$"more$$$${"" + text}"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$${"" + text}"<!>} padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${"more${"" + text}"} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$"more${"" + text}"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$$"more$${"" + text}"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$$$$"more$$$${"" + text}"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$${"" + text}"<!>} padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${"more${"" + text}"} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$"more${"" + text}"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$"more$${"" + text}"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$"more$$$${"" + text}"<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"more$$$$$$$${"" + text}"<!>} padding"""<!>



    """padding ${"""more$text"""} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$"""more$text"""<!>} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$$"""more$$text"""<!>} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$text"""<!>} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$text"""<!>} padding"""

    <!UNSUPPORTED_FEATURE!>$"""padding ${"""more$text"""} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$"""more$text"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$$"""more$$text"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$text"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$text"""<!>} padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$"""padding $${"""more$text"""} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$"""more$text"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$$"""more$$text"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$text"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$text"""<!>} padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${"""more$text"""} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$"""more$text"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$$"""more$$text"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$text"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$text"""<!>} padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${"""more$text"""} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$"""more$text"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$"""more$$text"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$text"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$text"""<!>} padding"""<!>


    """padding ${"""more$`text`"""} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$"""more$`text`"""<!>} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$$"""more$$`text`"""<!>} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$`text`"""<!>} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$`text`"""<!>} padding"""

    <!UNSUPPORTED_FEATURE!>$"""padding ${"""more$`text`"""} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$"""more$`text`"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$$"""more$$`text`"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$`text`"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$`text`"""<!>} padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$"""padding $${"""more$`text`"""} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$"""more$`text`"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$$"""more$$`text`"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$`text`"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$`text`"""<!>} padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${"""more$`text`"""} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$"""more$`text`"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$$"""more$$`text`"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$`text`"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$`text`"""<!>} padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${"""more$`text`"""} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$"""more$`text`"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$"""more$$`text`"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$$`text`"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$$`text`"""<!>} padding"""<!>


    """padding ${"""more${"" + text}"""} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$"""more${"" + text}"""<!>} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$$"""more$${"" + text}"""<!>} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$${"" + text}"""<!>} padding"""
    """padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$${"" + text}"""<!>} padding"""

    <!UNSUPPORTED_FEATURE!>$"""padding ${"""more${"" + text}"""} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$"""more${"" + text}"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$$"""more$${"" + text}"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$${"" + text}"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$"""padding ${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$${"" + text}"""<!>} padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$"""padding $${"""more${"" + text}"""} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$"""more${"" + text}"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$$"""more$${"" + text}"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$${"" + text}"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$${"" + text}"""<!>} padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${"""more${"" + text}"""} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$"""more${"" + text}"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$$"""more$${"" + text}"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$${"" + text}"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$${"" + text}"""<!>} padding"""<!>

    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${"""more${"" + text}"""} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$"""more${"" + text}"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$"""more$${"" + text}"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$"""more$$$${"" + text}"""<!>} padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${<!UNSUPPORTED_FEATURE!>$$$$$$$$"""more$$$$$$$${"" + text}"""<!>} padding"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// string literal kinds: single-line, multi-line
fun multilineInterpolation() {
    "padding ${
        0 + value
    } padding"
    <!UNSUPPORTED_FEATURE!>$"padding ${
    0 + value
    } padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding $${
    0 + value
    } padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${
    0 + value
    } padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${
    0 + value
    } padding"<!>

    """padding ${
        0 + value
    } padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding ${
    0 + value
    } padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding $${
    0 + value
    } padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${
    0 + value
    } padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${
    0 + value
    } padding"""<!>
}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line
fun multilineCommentsInsideStringsWithInterpolation() {
    "padding /* $value */ padding"
    <!UNSUPPORTED_FEATURE!>$"padding /* $value */ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding /* $$value */ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding /* $$$$value */ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding /* $$$$$$$$value */ padding"<!>

    "padding /* $`value` */ padding"
    <!UNSUPPORTED_FEATURE!>$"padding /* $`value` */ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding /* $$`value` */ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding /* $$$$`value` */ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding /* $$$$$$$$`value` */ padding"<!>

    "padding /* ${0 + value} */ padding"
    <!UNSUPPORTED_FEATURE!>$"padding /* ${0 + value} */ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$"padding /* $${0 + value} */ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$"padding /* $$$${0 + value} */ padding"<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding /* $$$$$$$${0 + value} */ padding"<!>


    """padding /* $value */ padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding /* $value */ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding /* $$value */ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding /* $$$$value */ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding /* $$$$$$$$value */ padding"""<!>

    """padding /* $`value` */ padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding /* $`value` */ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding /* $$`value` */ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding /* $$$$`value` */ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding /* $$$$$$$$`value` */ padding"""<!>

    """padding /* ${0 + value} */ padding"""
    <!UNSUPPORTED_FEATURE!>$"""padding /* ${0 + value} */ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$"""padding /* $${0 + value} */ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$"""padding /* $$$${0 + value} */ padding"""<!>
    <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding /* $$$$$$$${0 + value} */ padding"""<!>
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
@Annotation(<!UNSUPPORTED_FEATURE!>$"padding $compileTimeConstant padding"<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$"padding $$compileTimeConstant padding"<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$"padding $$$$compileTimeConstant padding"<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$compileTimeConstant padding"<!>)

@Annotation("padding $`compileTimeConstant` padding")
@Annotation(<!UNSUPPORTED_FEATURE!>$"padding $`compileTimeConstant` padding"<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$"padding $$`compileTimeConstant` padding"<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$"padding $$$$`compileTimeConstant` padding"<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$`compileTimeConstant` padding"<!>)

@Annotation("padding ${0 + compileTimeConstant} padding")
@Annotation(<!UNSUPPORTED_FEATURE!>$"padding ${0 + compileTimeConstant} padding"<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$"padding $${0 + compileTimeConstant} padding"<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$"padding $$$${0 + compileTimeConstant} padding"<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${0 + compileTimeConstant} padding"<!>)


@Annotation("""padding $compileTimeConstant padding""")
@Annotation(<!UNSUPPORTED_FEATURE!>$"""padding $compileTimeConstant padding"""<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$"""padding $$compileTimeConstant padding"""<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$compileTimeConstant padding"""<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$compileTimeConstant padding"""<!>)

@Annotation("""padding $`compileTimeConstant` padding""")
@Annotation(<!UNSUPPORTED_FEATURE!>$"""padding $`compileTimeConstant` padding"""<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$"""padding $$`compileTimeConstant` padding"""<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$`compileTimeConstant` padding"""<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$`compileTimeConstant` padding"""<!>)

@Annotation("""padding ${0 + compileTimeConstant} padding""")
@Annotation(<!UNSUPPORTED_FEATURE!>$"""padding ${0 + compileTimeConstant} padding"""<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$"""padding $${0 + compileTimeConstant} padding"""<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${0 + compileTimeConstant} padding"""<!>)
@Annotation(<!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${0 + compileTimeConstant} padding"""<!>)

fun stringsWithInterpolationAsValidAnnotationArguments() {}

// interpolation prefix length: 0, 1, 2, 4, 8
// interpolation kinds: of simple identifier, of identifier in backticks, of arbitrary expression
// string literal kinds: single-line, multi-line

const val stringWithInterpolationAsValidConstantInitializer01 = "padding $compileTimeConstant padding"
const val stringWithInterpolationAsValidConstantInitializer02 = <!UNSUPPORTED_FEATURE!>$"padding $compileTimeConstant padding"<!>
const val stringWithInterpolationAsValidConstantInitializer03 = <!UNSUPPORTED_FEATURE!>$$"padding $$compileTimeConstant padding"<!>
const val stringWithInterpolationAsValidConstantInitializer04 = <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$compileTimeConstant padding"<!>
const val stringWithInterpolationAsValidConstantInitializer05 = <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$compileTimeConstant padding"<!>

const val stringWithInterpolationAsValidConstantInitializer06 = "padding $`compileTimeConstant` padding"
const val stringWithInterpolationAsValidConstantInitializer07 = <!UNSUPPORTED_FEATURE!>$"padding $`compileTimeConstant` padding"<!>
const val stringWithInterpolationAsValidConstantInitializer08 = <!UNSUPPORTED_FEATURE!>$$"padding $$`compileTimeConstant` padding"<!>
const val stringWithInterpolationAsValidConstantInitializer09 = <!UNSUPPORTED_FEATURE!>$$$$"padding $$$$`compileTimeConstant` padding"<!>
const val stringWithInterpolationAsValidConstantInitializer10 = <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$$`compileTimeConstant` padding"<!>

const val stringWithInterpolationAsValidConstantInitializer11 = "padding ${0 + compileTimeConstant} padding"
const val stringWithInterpolationAsValidConstantInitializer12 = <!UNSUPPORTED_FEATURE!>$"padding ${0 + compileTimeConstant} padding"<!>
const val stringWithInterpolationAsValidConstantInitializer13 = <!UNSUPPORTED_FEATURE!>$$"padding $${0 + compileTimeConstant} padding"<!>
const val stringWithInterpolationAsValidConstantInitializer14 = <!UNSUPPORTED_FEATURE!>$$$$"padding $$$${0 + compileTimeConstant} padding"<!>
const val stringWithInterpolationAsValidConstantInitializer15 = <!UNSUPPORTED_FEATURE!>$$$$$$$$"padding $$$$$$$${0 + compileTimeConstant} padding"<!>


const val stringWithInterpolationAsValidConstantInitializer16 = """padding $compileTimeConstant padding"""
const val stringWithInterpolationAsValidConstantInitializer17 = <!UNSUPPORTED_FEATURE!>$"""padding $compileTimeConstant padding"""<!>
const val stringWithInterpolationAsValidConstantInitializer18 = <!UNSUPPORTED_FEATURE!>$$"""padding $$compileTimeConstant padding"""<!>
const val stringWithInterpolationAsValidConstantInitializer19 = <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$compileTimeConstant padding"""<!>
const val stringWithInterpolationAsValidConstantInitializer20 = <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$compileTimeConstant padding"""<!>

const val stringWithInterpolationAsValidConstantInitializer21 = """padding $`compileTimeConstant` padding"""
const val stringWithInterpolationAsValidConstantInitializer22 = <!UNSUPPORTED_FEATURE!>$"""padding $`compileTimeConstant` padding"""<!>
const val stringWithInterpolationAsValidConstantInitializer23 = <!UNSUPPORTED_FEATURE!>$$"""padding $$`compileTimeConstant` padding"""<!>
const val stringWithInterpolationAsValidConstantInitializer24 = <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$$`compileTimeConstant` padding"""<!>
const val stringWithInterpolationAsValidConstantInitializer25 = <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$$`compileTimeConstant` padding"""<!>

const val stringWithInterpolationAsValidConstantInitializer26 = """padding ${0 + compileTimeConstant} padding"""
const val stringWithInterpolationAsValidConstantInitializer27 = <!UNSUPPORTED_FEATURE!>$"""padding ${0 + compileTimeConstant} padding"""<!>
const val stringWithInterpolationAsValidConstantInitializer28 = <!UNSUPPORTED_FEATURE!>$$"""padding $${0 + compileTimeConstant} padding"""<!>
const val stringWithInterpolationAsValidConstantInitializer29 = <!UNSUPPORTED_FEATURE!>$$$$"""padding $$$${0 + compileTimeConstant} padding"""<!>
const val stringWithInterpolationAsValidConstantInitializer30 = <!UNSUPPORTED_FEATURE!>$$$$$$$$"""padding $$$$$$$${0 + compileTimeConstant} padding"""<!>
