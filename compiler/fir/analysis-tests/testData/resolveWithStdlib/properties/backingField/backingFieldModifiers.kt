class A {
    val a: Number
        <!INAPPLICABLE_BACKING_FIELD_MODIFIER!>abstract<!> field = 1

    val b: Number
        <!INAPPLICABLE_BACKING_FIELD_MODIFIER!>open<!> field = 1

    val c: Number
        <!INAPPLICABLE_BACKING_FIELD_MODIFIER!>final<!> field = 1

    val d: Number
        <!INAPPLICABLE_BACKING_FIELD_MODIFIER!>inline<!> field = 1

    val e: Number
        <!INAPPLICABLE_BACKING_FIELD_MODIFIER!>noinline<!> field = 1

    val f: Number
        <!INAPPLICABLE_BACKING_FIELD_MODIFIER!>crossinline<!> field = 1

    val g: Number
        <!INAPPLICABLE_BACKING_FIELD_MODIFIER!>tailrec<!> field = 1
}
