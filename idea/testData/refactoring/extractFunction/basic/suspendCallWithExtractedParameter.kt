// PARAM_TYPES: D
// PARAM_DESCRIPTOR: value-parameter d: D defined in test1
class D {
    suspend fun await() {}
}

// SIBLING:
suspend fun test1(d: D) {
    <selection>d.await()</selection>
}