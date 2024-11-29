// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

class A
class B

context(a: A, b: B)
fun test(){ }

context(a: A, b: B)
val property: String
    get() = ""

fun usage(){
    with(A()){
        <!NO_CONTEXT_ARGUMENT!>test<!>()
        <!NO_CONTEXT_ARGUMENT!>property<!>
    }
}

context(a: A)
fun usage2(){
    <!NO_CONTEXT_ARGUMENT!>test<!>()
    <!NO_CONTEXT_ARGUMENT!>property<!>
}
