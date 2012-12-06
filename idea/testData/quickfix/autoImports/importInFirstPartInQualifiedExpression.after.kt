// "Import" "true"
// ERROR: Unresolved reference: Some

package testing

import some.Some

fun testing() {
  <caret>Some.test()
}
