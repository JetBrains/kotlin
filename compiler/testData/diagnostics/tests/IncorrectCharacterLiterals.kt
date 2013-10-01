// http://youtrack.jetbrains.net/issue/KT-451
// KT-451 Incorrect character literals cause assertion failures

fun ff() {
    val <!UNUSED_VARIABLE!>b<!> = <!EMPTY_CHARACTER_LITERAL!>''<!>
    val <!UNUSED_VARIABLE!>c<!> = <!TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL!>'23'<!>
    val <!UNUSED_VARIABLE!>d<!> = <!INCORRECT_CHARACTER_LITERAL!>'a<!>
    val <!UNUSED_VARIABLE!>e<!> = <!INCORRECT_CHARACTER_LITERAL!>'ab<!>
    val <!UNUSED_VARIABLE!>f<!> = '<!ILLEGAL_ESCAPE!>\<!>'
}

fun test() {
    <!UNUSED_EXPRESSION!>'a'<!>
    <!UNUSED_EXPRESSION!>'\n'<!>
    <!UNUSED_EXPRESSION!>'\t'<!>
    <!UNUSED_EXPRESSION!>'\b'<!>
    <!UNUSED_EXPRESSION!>'\r'<!>
    <!UNUSED_EXPRESSION!>'\"'<!>
    <!UNUSED_EXPRESSION!>'\''<!>
    <!UNUSED_EXPRESSION!>'\\'<!>
    <!UNUSED_EXPRESSION!>'\$'<!>
    <!UNUSED_EXPRESSION!>'<!ILLEGAL_ESCAPE!>\x<!>'<!>
    <!UNUSED_EXPRESSION!>'<!ILLEGAL_ESCAPE!>\123<!>'<!>
    <!UNUSED_EXPRESSION!>'<!ILLEGAL_ESCAPE!>\ra<!>'<!>
    <!UNUSED_EXPRESSION!>'<!ILLEGAL_ESCAPE!>\000<!>'<!>
    <!UNUSED_EXPRESSION!>'<!ILLEGAL_ESCAPE!>\000<!>'<!>
    <!UNUSED_EXPRESSION!>'\u0000'<!>
    <!UNUSED_EXPRESSION!>'\u000a'<!>
    <!UNUSED_EXPRESSION!>'\u000A'<!>
    <!UNUSED_EXPRESSION!>'<!ILLEGAL_ESCAPE!>\u<!>'<!>
    <!UNUSED_EXPRESSION!>'<!ILLEGAL_ESCAPE!>\u0<!>'<!>
    <!UNUSED_EXPRESSION!>'<!ILLEGAL_ESCAPE!>\u00<!>'<!>
    <!UNUSED_EXPRESSION!>'<!ILLEGAL_ESCAPE!>\u000<!>'<!>
    <!UNUSED_EXPRESSION!>'<!ILLEGAL_ESCAPE!>\u000z<!>'<!>
    <!UNUSED_EXPRESSION!>'<!ILLEGAL_ESCAPE!>\\u000<!>'<!>
    <!UNUSED_EXPRESSION!>'<!ILLEGAL_ESCAPE!>\<!>'<!>
}
