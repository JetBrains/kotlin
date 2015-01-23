// !DIAGNOSTICS: -UNUSED_VARIABLE
// KT-4351 Cannot resolve reference to self in init of class local to function

fun f() {
    class MyClass() {
        {
            val x: MyClass = MyClass()
        }

        fun member() {
            val x: MyClass = MyClass()
        }
    }

    <!LOCAL_OBJECT_NOT_ALLOWED!>object MyObject<!> {
        {
            val obj: MyObject = MyObject
        }
    }

    val x: MyClass = MyClass()
}

val closure = {
    class MyClass {
        {
            val x: MyClass = MyClass()
        }
    }
}
