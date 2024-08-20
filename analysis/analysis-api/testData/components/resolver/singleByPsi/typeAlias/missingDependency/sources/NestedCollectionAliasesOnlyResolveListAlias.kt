// IGNORE_FE10

// MODULE: dependency1
// MODULE_KIND: Source
// FILE: StringAlias.kt
package dependency1

typealias StringAlias = String

// MODULE: dependency2
// MODULE_KIND: Source
// FILE: ListAlias.kt
package dependency2

typealias ListAlias<A> = List<A>

// MODULE: dependency3
// MODULE_KIND: Source
// FILE: SetAlias.kt
package dependency3

typealias SetAlias<A> = Set<A>

// MODULE: dependency4(dependency1, dependency2, dependency3)
// MODULE_KIND: Source
// FILE: MyInterface.kt
package dependency4

import dependency1.*
import dependency2.*
import dependency3.*

interface MyInterface {
    fun check(list: ListAlias<SetAlias<StringAlias>>)
}

// MODULE: main(dependency4, dependency2)
// FILE: main.kt
package main

import dependency4.*

fun checkTypeAlias(m: MyInterface) {
    m.c<caret>heck(emptyList())
}
