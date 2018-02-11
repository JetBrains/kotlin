// !WITH_NEW_INFERENCE
fun foo(n: Number) = n

fun test() {
    foo(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>'a'<!>)
    
    val c = 'c'
    foo(<!TYPE_MISMATCH!>c<!>)

    val d: Char? = 'd'
    foo(<!NI;TYPE_MISMATCH!><!OI;TYPE_MISMATCH!>d<!>!!<!>)
}
