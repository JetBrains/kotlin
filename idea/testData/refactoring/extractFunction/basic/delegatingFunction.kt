// PARAM_DESCRIPTOR: val t: <no name provided> defined in foo
// PARAM_TYPES: T
interface T {
    fun test() {}
}

fun foo() {
    val t = object: T {}
    <selection>(object: T by t {}).test()</selection>
}