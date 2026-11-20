// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// RENDER_DIAGNOSTIC_ARGUMENTS
// FILE: test.kt
package `a{z}`
class `a'b`<T>
class `a''b`<T>

class A

fun <`{y}`> test(y: `{y}`) {
    class `{x}`
    mutableListOf<Int>().add(<!ARGUMENT_TYPE_MISMATCH("{x}<{y} (of fun <{y}> test)>; Int;  value parameter 'element' of;  '`{x}`()'")!>`{x}`()<!>)
    mutableListOf<Int>().add(<!ARGUMENT_TYPE_MISMATCH("{y} (of fun <{y}> test); Int;  value parameter 'element' of;  'y'")!>y<!>)
    mutableListOf<A>().add(<!ARGUMENT_TYPE_MISMATCH("b.A; a{z}.A;  value parameter 'element' of;  'b.A()'")!>b.A()<!>)
    mutableListOf<Int>().add(<!ARGUMENT_TYPE_MISMATCH("a'b<String>; Int;  value parameter 'element' of;  '`a\'b`<String>()'")!>`a'b`<String>()<!>)
    mutableListOf<Int>().add(<!ARGUMENT_TYPE_MISMATCH("a''b<String>; Int;  value parameter 'element' of;  '`a\'\'b`<String>()'")!>`a''b`<String>()<!>)
}

// FILE: b.kt
package b

class A

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass */
