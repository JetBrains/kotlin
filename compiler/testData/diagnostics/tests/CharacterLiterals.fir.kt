fun test(c : Char) {
  test('')
  test('a')
  test('aa')
  <!INAPPLICABLE_CANDIDATE!>test<!>('a)
  <!UNRESOLVED_REFERENCE!>test<!>('
  <!UNRESOLVED_REFERENCE!>test<!>(0<!SYNTAX!><!>'
  <!UNRESOLVED_REFERENCE!>test<!>('\n')
  <!UNRESOLVED_REFERENCE!>test<!>('\\')
  <!UNRESOLVED_REFERENCE!>test<!>(''<!SYNTAX!><!>'')
  test('\'')
  test('\"')
}
