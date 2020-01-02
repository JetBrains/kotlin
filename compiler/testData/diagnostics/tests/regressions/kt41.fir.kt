// KT-41 Make functions with errors in returning statement return ERROR type and not Nothing

package kt41

fun aaa() =
  6 <!UNRESOLVED_REFERENCE!>foo<!> 1

fun bbb() {
  aaa()
  1 // Stupid error: unreachable code
}
