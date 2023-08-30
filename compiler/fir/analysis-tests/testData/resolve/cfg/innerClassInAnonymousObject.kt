// !DUMP_CFG

val x = object {
    <!NESTED_CLASS_NOT_ALLOWED!>class Nested<!> {
        fun foo() {}
    }
}
