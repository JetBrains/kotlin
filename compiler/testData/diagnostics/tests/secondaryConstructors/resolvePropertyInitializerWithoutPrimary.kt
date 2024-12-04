// RUN_PIPELINE_TILL: FRONTEND
class A {
    val prop: Int = <!TYPE_MISMATCH!>""<!>
    constructor()
}
