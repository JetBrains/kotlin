import kotlin.text.*

const val a = <!EVALUATED: `IGNORE_CASE`!>RegexOption.IGNORE_CASE.name<!>
const val b = <!EVALUATED: `0`!>RegexOption.IGNORE_CASE.ordinal<!>
const val c = <!EVALUATED: `MULTILINE`!>RegexOption.MULTILINE.name<!>
const val d = <!EVALUATED: `1`!>RegexOption.MULTILINE.ordinal<!>
const val e = <!EVALUATED: `true`!>RegexOption.IGNORE_CASE == RegexOption.IGNORE_CASE<!>
const val f = <!EVALUATED: `true`!>RegexOption.IGNORE_CASE != RegexOption.MULTILINE<!>
const val g = <!EVALUATED: `IGNORE_CASE`!>RegexOption.IGNORE_CASE.toString()<!>
