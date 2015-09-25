// http://youtrack.jetbrains.net/issue/KT-421
// KT-421 Strange 'unresolved' bug with backing fields

class A() {
    val c = 1
    val a = <!UNINITIALIZED_VARIABLE!>b<!>
    val b = <!BACKING_FIELD_USAGE_DEPRECATED!>$c<!>  // '$c' is unresolved
}
