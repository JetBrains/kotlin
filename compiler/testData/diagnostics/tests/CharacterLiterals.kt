fun test(<!UNUSED_PARAMETER!>c<!> : Char) {
  test(<!ERROR_COMPILE_TIME_VALUE!>''<!>)
  test('a')
  test(<!ERROR_COMPILE_TIME_VALUE!>'aa'<!>)
  test(<!ERROR_COMPILE_TIME_VALUE!>'a)<!>
  <!UNRESOLVED_REFERENCE!>test<!>(<!ERROR_COMPILE_TIME_VALUE!>'<!>
  <!UNRESOLVED_REFERENCE!>test<!>(0<!SYNTAX!><!SYNTAX!><!>'<!>
  test('\n')
  test('\\')
  test(<!ERROR_COMPILE_TIME_VALUE!>''<!><!SYNTAX!>''<!><!SYNTAX!>)<!>
  test('\'')
  test('\"')
}
