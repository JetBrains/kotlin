// http://youtrack.jetbrains.net/issue/KT-451
// KT-451 Incorrect character literals cause assertion failures

fun ff() {
    val b = <!ILLEGAL_CONST_EXPRESSION!>''<!>
    val c = <!ILLEGAL_CONST_EXPRESSION!>'23'<!>
    val d = <!ILLEGAL_CONST_EXPRESSION!>'a<!>
    val e = <!ILLEGAL_CONST_EXPRESSION!>'ab<!>
    val f = <!ILLEGAL_CONST_EXPRESSION!>'\'<!>
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
    <!ILLEGAL_CONST_EXPRESSION!>'\x'<!>
    <!ILLEGAL_CONST_EXPRESSION!>'\123'<!>
    <!ILLEGAL_CONST_EXPRESSION!>'\ra'<!>
    <!ILLEGAL_CONST_EXPRESSION!>'\000'<!>
    <!ILLEGAL_CONST_EXPRESSION!>'\000'<!>
    '\u0000'
    '\u000a'
    '\u000A'
    <!ILLEGAL_CONST_EXPRESSION!>'\u'<!>
    <!ILLEGAL_CONST_EXPRESSION!>'\u0'<!>
    <!ILLEGAL_CONST_EXPRESSION!>'\u00'<!>
    <!ILLEGAL_CONST_EXPRESSION!>'\u000'<!>
    <!ILLEGAL_CONST_EXPRESSION!>'\u000z'<!>
    <!ILLEGAL_CONST_EXPRESSION!>'\\u000'<!>
    <!ILLEGAL_CONST_EXPRESSION!>'\'<!>
}
