// KT-65038

// IGNORE_FE10

// MODULE: dependency1
// MODULE_KIND: Source
// FILE: MyString.kt
package dependency1

typealias MyString = String

// MODULE: dependency2(dependency1)
// MODULE_KIND: Source
// FILE: MyInterface.kt
package dependency2

import dependency1.*

interface MyInterface {
    fun check(string: MyString)
}

// MODULE: main(dependency2)
// FILE: main.kt
package main

import dependency2.*

fun checkTypeAlias(m: MyInterface) {
    m.c<caret>heck("")
}
