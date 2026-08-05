// MODULE: dep
// FILE: base.kt
package test

interface A {
    fun foo(): String
}

interface B {
    fun foo(): String
}

// FILE: derived.kt
package test

interface C : A, B

// MODULE: main(dep)
// FILE: main.kt
package test

fun usage(c: C) {
    c.fo<caret>o()
}
