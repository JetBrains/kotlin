// FILE: a.kt
package a

class AliasedContainer {
    class AliasedF
}

// Matching names are essential.
typealias AliasedF = AliasedContainer.AliasedF

// FILE:main.kt
package b

import a.AliasedF

fun referAlias3(pf: AliasedF) {
    <expr>a.AliasedContainer.AliasedF()</expr>
}
