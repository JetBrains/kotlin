//KT-3301 Inference with several supertypes fails

package arrays

interface A
interface B

object CAB : A, B
object DAB : A, B

fun m(<!UNUSED_PARAMETER!>args<!> : Array<A>) {

}

fun test122() {
    m(array(CAB, DAB)) // Wrong error here: Array<Any> is inferred while expected Array<A> is satisfied
}

//from library
fun array<T>(vararg <!UNUSED_PARAMETER!>t<!>: T): Array<T> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>