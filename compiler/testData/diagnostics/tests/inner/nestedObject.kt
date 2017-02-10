// SKIP_TXT
class Outer {
    inner class Inner1 {
        <!NESTED_OBJECT_NOT_ALLOWED!>object Obj1<!>

        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object Obj2

        inner class Inner2 {
            <!NESTED_OBJECT_NOT_ALLOWED!>object Obj3<!>
        }
    }
}
