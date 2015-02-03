// PARAM_DESCRIPTOR: local final class <no name provided> defined in x
// PARAM_TYPES: kotlin.Any
// WITH_RUNTIME

// SIBLING:
val x = object {
    fun test() {
        <selection>println(this)</selection>
    }
}