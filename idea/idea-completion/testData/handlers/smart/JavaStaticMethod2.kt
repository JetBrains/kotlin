fun foo(r: Runnable){}

fun bar(){
    foo(<caret>)
}


// INVOCATION_COUNT: 2
// ELEMENT_TEXT: Thread.currentThread
