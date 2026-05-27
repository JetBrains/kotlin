// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// LANGUAGE: +DirectClassInheritors

// MODULE: lib
// FILE: Lib.kt
package lib

abstract class A
interface I<T>

// MODULE: main(lib)
// FILE: Main.kt
package main

import lib.*
class B : A(), I<Int>

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration */
