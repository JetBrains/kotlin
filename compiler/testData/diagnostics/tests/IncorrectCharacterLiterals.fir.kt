// http://youtrack.jetbrains.net/issue/KT-451
// KT-451 Incorrect character literals cause assertion failures

fun ff() {
    val b = <!EMPTY_CHARACTER_LITERAL!>''<!>
    val c = <!TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL!>'23'<!>
    val d = <!INCORRECT_CHARACTER_LITERAL!>'a<!>
    val e = <!INCORRECT_CHARACTER_LITERAL!>'ab<!>
    val f = <!ILLEGAL_ESCAPE!>'\'<!>
}

fun test() {
    'a'
    '\n'
    '\t'
    '\b'
    '\r'
    '\"'
    '\''
    '\\'
    '\$'
    <!ILLEGAL_ESCAPE!>'\x'<!>
    <!ILLEGAL_ESCAPE!>'\123'<!>
    <!ILLEGAL_ESCAPE!>'\ra'<!>
    <!ILLEGAL_ESCAPE!>'\000'<!>
    <!ILLEGAL_ESCAPE!>'\000'<!>
    '\u0000'
    '\u000a'
    '\u000A'
    <!ILLEGAL_ESCAPE!>'\u'<!>
    <!ILLEGAL_ESCAPE!>'\u0'<!>
    <!ILLEGAL_ESCAPE!>'\u00'<!>
    <!ILLEGAL_ESCAPE!>'\u000'<!>
    <!ILLEGAL_ESCAPE!>'\u000z'<!>
    <!ILLEGAL_ESCAPE!>'\\u000'<!>
    <!ILLEGAL_ESCAPE!>'\'<!>
}
