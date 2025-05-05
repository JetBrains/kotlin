// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76839

// MODULE: baseLib
open class A

// MODULE: lib(baseLib)
interface B

class C : A(), B

fun foo(f: (C) -> Unit) {
    f(C())
}

// MODULE: main(lib)
data class Some(<!MISSING_DEPENDENCY_SUPERCLASS!>val x: C<!>) // crashes backend

fun test() {
    foo {
        it.hashCode() // crashes backend
    }
}
