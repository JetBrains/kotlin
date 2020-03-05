// http://youtrack.jetbrains.net/issue/KT-451
// KT-451 Incorrect character literals cause assertion failures

fun ff() {
    val b = ''
    val c = '23'
    val d = 'a
    val e = 'ab
    val f = '\'
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
    '\x'
    '\123'
    '\ra'
    '\000'
    '\000'
    '\u0000'
    '\u000a'
    '\u000A'
    '\u'
    '\u0'
    '\u00'
    '\u000'
    '\u000z'
    '\\u000'
    '\'
}
