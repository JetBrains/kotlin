object A {
    object B {
        object C
    }

    interface X

    val a = object : Any() {
        <!LOCAL_OBJECT_NOT_ALLOWED!>object D<!> {
            <!LOCAL_OBJECT_NOT_ALLOWED!>object G<!>
            <!NESTED_CLASS_NOT_ALLOWED!>interface Z<!>
        }

        <!NESTED_CLASS_NOT_ALLOWED!>interface Y<!>
    }

    fun b() {
        <!LOCAL_OBJECT_NOT_ALLOWED!>object E<!> {
            <!LOCAL_OBJECT_NOT_ALLOWED!>object F<!>
            <!NESTED_CLASS_NOT_ALLOWED!>interface M<!>
        }

        <!LOCAL_INTERFACE_NOT_ALLOWED!>interface N<!>

        val c = object : Any() {
            val t = "test"

            <!NESTED_CLASS_NOT_ALLOWED!>interface U<!>
        }
    }
}
