// FIR_IDENTICAL
package foo

open class Super {
    <!JS_NAME_CLASH!>val foo<!> = 23
}

class Sub : Super() {
    <!JS_NAME_CLASH!>fun foo()<!> = 42
}