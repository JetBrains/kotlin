// FIR_COMPARISON
package test

class AClass

val AClass.ext get() = this

fun usage(a: AClass) {
    a.ex<caret>
}

// ELEMENT: ext
