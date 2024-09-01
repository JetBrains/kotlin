// IGNORE_FE10

// MODULE: dependency1
// MODULE_KIND: Source
// FILE: BaseAliases.kt
package dependency1

typealias IntAlias = Int
typealias BooleanAlias = Boolean

// MODULE: dependency2
// MODULE_KIND: Source
// FILE: AsymmetricAlias.kt
package dependency2

typealias AsymmetricAlias<A, B> = String

// MODULE: dependency3
// MODULE_KIND: Source
// FILE: AsymmetricAlias2.kt
package dependency3

typealias AsymmetricAlias2<A, B> = List<B>

// MODULE: dependency4(dependency1, dependency2, dependency3)
// MODULE_KIND: Source
// FILE: MyInterface.kt
package dependency4

import dependency1.*
import dependency2.*
import dependency3.*

interface MyInterface {
    fun check(list: AsymmetricAlias2<IntAlias, AsymmetricAlias<BooleanAlias, IntAlias>>)
}

// MODULE: main(dependency4, dependency2)
// FILE: main.kt
package main

import dependency4.*

fun checkTypeAlias(m: MyInterface) {
    m.c<caret>heck(emptyList())
}
