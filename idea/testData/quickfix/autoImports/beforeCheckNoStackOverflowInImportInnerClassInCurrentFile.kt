// "class com.intellij.codeInsight.daemon.impl.quickfix.ImportClassFixBase" "false"

// KT-3165 Weird stack overflow in IDE
// ERROR: Unresolved reference: Bar
// ERROR: Unresolved reference: SomeImpossibleName

import Foo.Bar

class Foo

fun f() {
    <caret>SomeImpossibleName
}