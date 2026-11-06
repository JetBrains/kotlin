// MODULE: base
// FILE: Foo.kt
package base

class Foo

// MODULE: expansion(base)
// FILE: expansion.kt
package expansion

import base.Foo

val propertyWithWrongTypeArguments: Foo<Int> = TODO()

// MODULE: main(expansion)
// FILE: main.kt
package base

import expansion.propertyWithWrongTypeArguments

// Has the same class ID as `base.Foo` from the `base` module, which `main` does not depend on, but a different number of type parameters
class Foo<T>

fun test() {
    p<caret_type>ropertyWithWrongTypeArguments.toString()
}
