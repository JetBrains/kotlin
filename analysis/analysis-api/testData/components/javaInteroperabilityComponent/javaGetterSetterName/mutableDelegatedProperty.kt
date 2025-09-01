// WITH_STDLIB

import kotlin.properties.Delegates

class Foo {
    <expr>var x by Delegates.vetoable("x") { _, _, _ -> false }</expr>
}