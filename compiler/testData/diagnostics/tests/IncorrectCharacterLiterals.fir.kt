// http://youtrack.jetbrains.net/issue/KT-451
// KT-451 Incorrect character literals cause assertion failures

fun ff() {
    val b = <!INFERENCE_ERROR, INFERENCE_ERROR!>''<!>
    val c = <!INFERENCE_ERROR, INFERENCE_ERROR!>'23'<!>
    val d = <!INFERENCE_ERROR, INFERENCE_ERROR!>'a<!>
    val e = <!INFERENCE_ERROR, INFERENCE_ERROR!>'ab<!>
    val f = <!INFERENCE_ERROR, INFERENCE_ERROR!>'\'<!>
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
    <!INFERENCE_ERROR!>'\x'<!>
    <!INFERENCE_ERROR!>'\123'<!>
    <!INFERENCE_ERROR!>'\ra'<!>
    <!INFERENCE_ERROR!>'\000'<!>
    <!INFERENCE_ERROR!>'\000'<!>
    '\u0000'
    '\u000a'
    '\u000A'
    <!INFERENCE_ERROR!>'\u'<!>
    <!INFERENCE_ERROR!>'\u0'<!>
    <!INFERENCE_ERROR!>'\u00'<!>
    <!INFERENCE_ERROR!>'\u000'<!>
    <!INFERENCE_ERROR!>'\u000z'<!>
    <!INFERENCE_ERROR!>'\\u000'<!>
    <!INFERENCE_ERROR, INFERENCE_ERROR!>'\'<!>
}
