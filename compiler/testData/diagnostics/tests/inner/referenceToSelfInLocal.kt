// !DIAGNOSTICS: -UNUSED_VARIABLE
// KT-4351 Cannot resolve reference to self in init of class local to function

fun f() {
    class MyClass() {
        init {
            val x: MyClass = MyClass()
        }

        fun member() {
            val x: MyClass = MyClass()
        }
    }

    <!LOCAL_OBJECT_NOT_ALLOWED!>object MyObject<!> {
        init {
            val obj: MyObject = MyObject
        }
    }

    val x: MyClass = MyClass()
}

val closure = {
    class MyClass {
        init {
            val x: MyClass = MyClass()
        }
    }
}
