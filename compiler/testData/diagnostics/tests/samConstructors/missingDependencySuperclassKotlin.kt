// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81075

// MODULE: dep
// FILE: dep.kt
package dep

interface Callback {
    fun foo() {}
}

interface AbstractCallback {
    fun callback(isDark: Boolean)
}

// MODULE: lib(dep)
// FILE: lib.kt
package lib

import dep.Callback
import dep.AbstractCallback

fun interface ThemeCallback : Callback {
    fun callback(isDark: Boolean)
}

fun interface InheritedCallback : AbstractCallback

fun takeTheme(c: ThemeCallback) {}
fun takeInherited(c: InheritedCallback) {}

// MODULE: app(lib)
// FILE: main.kt
import lib.*

val themeCallback = ThemeCallback {}
val themeCallbackConversion: ThemeCallback = {}

val inheritedCallback = <!INTERFACE_AS_FUNCTION!>InheritedCallback<!> {}
val inheritedCallbackConversion: InheritedCallback <!INITIALIZER_TYPE_MISMATCH!>=<!> {}

fun box(): String {
    takeTheme {}
    takeInherited <!ARGUMENT_TYPE_MISMATCH!>{}<!>
    return "OK"
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, interfaceDeclaration, lambdaLiteral, propertyDeclaration,
samConversion, stringLiteral */
