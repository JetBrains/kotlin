// PARAM_DESCRIPTOR: val x: <no name provided> defined in test
// PARAM_TYPES: A
// WITH_RUNTIME

open class A {

}

fun foo(a: A) {

}

// SIBLING:
fun test() {
    val x = object: A() { }
    <selection>foo(x)</selection>
}