fun test(c : Char) {
  test(<!INFERENCE_ERROR!>''<!>)
  test('a')
  test(<!INFERENCE_ERROR!>'aa'<!>)
  <!INAPPLICABLE_CANDIDATE!>test<!>(<!INFERENCE_ERROR!>'a)<!>
  <!UNRESOLVED_REFERENCE!>test<!>(<!INFERENCE_ERROR!>'<!>
  <!UNRESOLVED_REFERENCE!>test<!>(0<!INFERENCE_ERROR!><!SYNTAX!><!>'<!>
  <!UNRESOLVED_REFERENCE!>test<!>('\n')
  <!UNRESOLVED_REFERENCE!>test<!>('\\')
  <!UNRESOLVED_REFERENCE!>test<!>(<!INFERENCE_ERROR!>''<!><!INFERENCE_ERROR!><!SYNTAX!><!>''<!>)
  test('\'')
  test('\"')
}
