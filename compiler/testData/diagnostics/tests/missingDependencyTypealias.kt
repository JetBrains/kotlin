// RUN_PIPELINE_TILL: BACKEND
// SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE: KT-69769
// FIR_IDENTICAL
// ISSUE: KT-65038

// MODULE: base1
// FILE: MyString.kt
package base1

typealias MyString = String

// MODULE: base2(base1)
// FILE: MyInterface.kt
package base2

import base1.MyString

interface MyInterface {
    fun check(string: MyString)
}

// MODULE: main(base2)
// FILE: main.kt
package main

import base2.MyInterface

fun checkTypeAlias(m: MyInterface) {
    m.check("")
}
