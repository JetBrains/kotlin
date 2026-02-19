// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-20278

// FILE: test1.kt
package test1

class A {
    inner class B<T>
    fun test(x: Any) = x is <!NO_TYPE_ARGUMENTS_ON_RHS!>B<!>
}

// FILE: test2.kt
package test2

class A<T> {
    inner class B
    fun test(x: Any) = x is <!NO_TYPE_ARGUMENTS_ON_RHS!>B<!>
}

// FILE: test3.kt
package test3

class A {
    class B {
        inner class C<T> {
            inner class D
            fun test(x: Any) = x is <!NO_TYPE_ARGUMENTS_ON_RHS!>D<!>
        }
    }
}

// FILE: test4.kt
package test4

class A {
    class B<T> {
        inner class C<U> {
            inner class D
            fun test(x: Any) = x is <!NO_TYPE_ARGUMENTS_ON_RHS!>D<!>
        }
    }
}

// FILE: test5.kt
package test5

class A {
    class B<T> {
        inner class C<U> {
            inner class D
        }
    }
    fun test(x: Any) = x is <!NO_TYPE_ARGUMENTS_ON_RHS!>B.C.D<!>
}

// FILE: test6.kt
package test6

class A {
    class B<T> {
        inner class C<U> {
            inner class D<V>
        }
    }
    fun test(x: Any) = x is <!NO_TYPE_ARGUMENTS_ON_RHS!>B.C.D<!>
}

// FILE: test7.kt
package test7

class A {
    class B<T> {
        class C<U> {
            inner class D
        }
    }
    fun test(x: Any) = x is <!NO_TYPE_ARGUMENTS_ON_RHS!>B.C.D<!>
}

// FILE: test8.kt
package test8

class A<T, U> {
    inner class B<V, W> {
        inner class C<X, Y>
        fun test(x: Any) = x is <!NO_TYPE_ARGUMENTS_ON_RHS!>C<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inner, isExpression, nullableType, typeParameter */
