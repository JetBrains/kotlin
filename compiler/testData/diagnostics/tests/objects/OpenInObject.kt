object Obj {
    fun foo() {}

    <!NON_FINAL_MEMBER_IN_OBJECT!>open<!> fun bar() {}

    var x: Int = 0

    <!NON_FINAL_MEMBER_IN_OBJECT!>open<!> var y: Int = 1
}
