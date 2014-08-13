// "Import" "true"
// ERROR: Unresolved reference: someTestProp

package test

import test.data.someTestProp

fun foo() {
    someTestProp
}
