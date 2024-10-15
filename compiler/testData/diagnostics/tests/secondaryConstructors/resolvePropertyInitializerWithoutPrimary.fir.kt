// RUN_PIPELINE_TILL: SOURCE
class A {
    val prop: Int = <!INITIALIZER_TYPE_MISMATCH!>""<!>
    constructor()
}
