// "Import Class" "false"

// KT-3165 Weird stack overflow in IDE
// ERROR: Unresolved reference: Bar
// ERROR: Unresolved reference: SomeImpossibleName

import Foo.Bar

class Foo

fun f() {
    <caret>SomeImpossibleName
}