// RUN_PIPELINE_TILL: SOURCE
class A {
    val prop: Int = <!TYPE_MISMATCH!>""<!>
    constructor()
}
