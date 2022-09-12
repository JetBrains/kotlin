// !DIAGNOSTICS: -UNUSED_PARAMETER
@DslMarker
annotation class Ann

@Ann
class A {
    var a = 1
}

var A.a1: Int
    get() = 1
    set(value) {}

@Ann
class B {
    var b = 2
}

var B.b1: Int
    get() = 1
    set(value) {}

fun foo(x: A.() -> Unit) {}
fun bar(x: B.() -> Unit) {}

fun test() {
    foo {
        a + 1
        a += a + 1
        a++

        a1 + 1
        a1 += a1 + 1
        a1++

        bar {
            <!DSL_SCOPE_VIOLATION!>a<!> + 1
            <!DSL_SCOPE_VIOLATION, DSL_SCOPE_VIOLATION!>a<!> += <!DSL_SCOPE_VIOLATION!>a<!> + 1
            <!DSL_SCOPE_VIOLATION, DSL_SCOPE_VIOLATION!>a<!>++

            <!DSL_SCOPE_VIOLATION!>a1<!> + 1
            <!DSL_SCOPE_VIOLATION!>a1<!> += <!DSL_SCOPE_VIOLATION!>a1<!> + 1
            <!DSL_SCOPE_VIOLATION!>a1<!>++

            this@foo.a + 1
            this@foo.a += this@foo.a + 1
            this@foo.a++

            this@foo.a1 + 1
            this@foo.a1 += this@foo.a1 + 1
            this@foo.a1++

            b + 1
            b += b + 1
            b++

            b1 + 1
            b1 += b1 + 1
            b1++
        }
    }
}
