// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-28132

// KT-28132: Members of enum with same name as class imported from other package try to call that constructor

// FILE: test0/Foo.kt
package test0

class Foo

// FILE: test1/Foo.kt
package test1

import test0.Foo

enum class Foo(val x: Int) {
    A(3)
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, primaryConstructor, propertyDeclaration */
