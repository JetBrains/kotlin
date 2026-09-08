// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// RENDER_DIAGNOSTIC_ARGUMENTS
// FILE: test.kt
package `a{z}`

class A

fun <`{y}`> test(y: `{y}`) {
    class `{x}`
    mutableListOf<Int>().add(<!ARGUMENT_TYPE_MISMATCH("{x}<{y} (of fun <{y}> test)>; Int")!>`{x}`()<!>)
    mutableListOf<Int>().add(<!ARGUMENT_TYPE_MISMATCH("{y} (of fun <{y}> test); Int")!>y<!>)
    mutableListOf<A>().add(<!ARGUMENT_TYPE_MISMATCH("b.A; a{z}.A")!>b.A()<!>)
}

// FILE: b.kt
package b

class A

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass */
