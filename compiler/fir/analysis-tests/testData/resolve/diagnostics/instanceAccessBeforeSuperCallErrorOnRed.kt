// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-72246

class Foo(a: Any) {
    constructor(): this(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>{ data: Int -> }()<!><!SYNTAX!><!>
}
