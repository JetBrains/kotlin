class A {
    val a: Number
        <!WRONG_MODIFIER_TARGET!>abstract<!> field = 1

    val b: Number
        <!WRONG_MODIFIER_TARGET!>open<!> field = 1

    val c: Number
        <!WRONG_MODIFIER_TARGET!>final<!> field = 1

    val d: Number
        <!WRONG_MODIFIER_TARGET!>inline<!> field = 1

    val e: Number
        <!WRONG_MODIFIER_TARGET!>noinline<!> field = 1

    val f: Number
        <!WRONG_MODIFIER_TARGET!>crossinline<!> field = 1

    val g: Number
        <!WRONG_MODIFIER_TARGET!>tailrec<!> field = 1
}
