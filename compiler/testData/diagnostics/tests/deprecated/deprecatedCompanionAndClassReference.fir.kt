// ISSUE: KT-54209

class A {
    @Deprecated("Deprecated companion")
    companion object
}


fun test() {
    <!DEPRECATION!>A<!>::class
    A.<!DEPRECATION!>Companion<!>::class
}
