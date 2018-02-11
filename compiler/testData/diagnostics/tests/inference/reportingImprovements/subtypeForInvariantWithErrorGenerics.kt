// !WITH_NEW_INFERENCE
package a

fun <R> foo (f: ()->R, r: MutableList<R>) = r.add(f())
fun <R> bar (r: MutableList<R>, f: ()->R) = r.add(f())

fun test() {
    val <!UNUSED_VARIABLE!>a<!> = <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>foo<!>({1}, arrayListOf("")) //no type inference error on 'arrayListOf'
    val <!UNUSED_VARIABLE!>b<!> = <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>bar<!>(arrayListOf(""), {1})
}

// from standard library
fun <T> arrayListOf(vararg <!UNUSED_PARAMETER!>values<!>: T) : MutableList<T> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>