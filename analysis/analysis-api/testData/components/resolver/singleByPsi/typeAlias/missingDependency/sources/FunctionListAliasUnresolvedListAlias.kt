// IGNORE_FE10

// MODULE: dependency1
// MODULE_KIND: Source
// FILE: FunctionAlias.kt
package dependency1

typealias FunctionAlias = (String) -> Int

// MODULE: dependency2
// MODULE_KIND: Source
// FILE: ListAlias.kt
package dependency2

typealias ListAlias<A> = List<A>

// MODULE: dependency3(dependency1, dependency2)
// MODULE_KIND: Source
// FILE: MyInterface.kt
package dependency3

import dependency1.*
import dependency2.*

interface MyInterface {
    fun check(list: ListAlias<FunctionAlias>)
}

// MODULE: main(dependency3, dependency1)
// FILE: main.kt
package main

import dependency3.*

fun checkTypeAlias(m: MyInterface) {
    m.c<caret>heck(emptyList())
}
