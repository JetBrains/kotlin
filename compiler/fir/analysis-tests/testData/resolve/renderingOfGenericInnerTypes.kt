// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// RENDER_DIAGNOSTIC_ARGUMENTS
package foo

class C<T> {
    inner class D<R>
    inner class D2<R, S> {
        inner class E<V, W, Y>
    }

    class NonInner<R>
}

class D

fun test() {
    val d: D <!INITIALIZER_TYPE_MISMATCH("D; C<String>.D<Int>")!>=<!> C<String>().D<Int>()
    val d2: D <!INITIALIZER_TYPE_MISMATCH("D; C<String>.D2<Int, Boolean>.E<Char, Long, Short>")!>=<!> C<String>().D2<Int, Boolean>().E<Char, Long, Short>()
    val d3: D <!INITIALIZER_TYPE_MISMATCH("D; C.NonInner<String>")!>=<!> C.NonInner<String>()

    fun <X> genTest() {
        class Local
        val d: D <!INITIALIZER_TYPE_MISMATCH("D; Local<X (of fun <X> genTest)>")!>=<!> Local()

        class Local2<T> {
            inner class Inner
        }

        val d2: D <!INITIALIZER_TYPE_MISMATCH("D; Local2<String, X (of fun <X> genTest)>.Inner")!>=<!> Local2<String>().Inner()
    }
}

private fun <T> veryCunningCallee() = run {
    class C {
        fun takeSame(c: C) {
        }
    }
    C()
}

class X<T> {
    private fun <T> veryCunningCallee() = run {
        class C<R> {
            inner class Inner {
                fun takeSame(inner: Inner) {
                }
            }
        }
        C<Long>().Inner()
    }
}

fun caller() {
    veryCunningCallee<Int>().takeSame(<!ARGUMENT_TYPE_MISMATCH("C<String>; C<Int>")!>veryCunningCallee<String>()<!>)
    X<Boolean>().<!INVISIBLE_REFERENCE("fun <T> veryCunningCallee(): C.Inner<Long, T, Boolean>; private; 'foo.X'")!>veryCunningCallee<!><String>().takeSame(<!ARGUMENT_TYPE_MISMATCH("C<Long, String, Short>.Inner; C<Long, String, Boolean>.Inner")!>X<Short>().<!INVISIBLE_REFERENCE("fun <T> veryCunningCallee(): C.Inner<Long, T, Short>; private; 'foo.X'")!>veryCunningCallee<!><String>()<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, nestedClass, propertyDeclaration */
