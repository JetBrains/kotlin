// FIR_IDENTICAL
package foo

interface I {
    <!JS_NAME_CLASH!>fun foo()<!> = 23
}

class Sub : I {
    <!JS_NAME_CLASH!>var foo<!> = 42
}