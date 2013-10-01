fun test(<!UNUSED_PARAMETER!>c<!> : Char) {
  test(<!EMPTY_CHARACTER_LITERAL!>''<!>)
  test('a')
  test(<!TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL!>'aa'<!>)
  test(<!INCORRECT_CHARACTER_LITERAL!>'a)<!>
  <!UNRESOLVED_REFERENCE!>test<!>(<!INCORRECT_CHARACTER_LITERAL!>'<!>
  <!UNRESOLVED_REFERENCE!>test<!>(0<!SYNTAX!><!SYNTAX!><!>'<!>
  test('\n')
  test('\\')
  test(<!EMPTY_CHARACTER_LITERAL!>''<!><!SYNTAX!>''<!><!SYNTAX!>)<!>
  test('\'')
  test('\"')
}
