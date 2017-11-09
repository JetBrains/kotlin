// "Import" "false"
// WITH_RUNTIME
// ACTION: Create local variable 'CoroutineImpl'
// ACTION: Create object 'CoroutineImpl'
// ACTION: Create parameter 'CoroutineImpl'
// ACTION: Create property 'CoroutineImpl'
// ACTION: Introduce local variable
// ACTION: Create annotation 'CoroutineImpl'
// ACTION: Create class 'CoroutineImpl'
// ACTION: Create enum 'CoroutineImpl'
// ACTION: Create interface 'CoroutineImpl'
// ACTION: Rename reference
// ERROR: Unresolved reference: CoroutineImpl

fun some() {
    CoroutineImpl<caret>::class
}
