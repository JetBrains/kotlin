// !LANGUAGE: +InlineClasses

inline class UInt(val a: Int) {
    fun test() {
        takeNullable(this) // box
        takeAnyInside(this) // box

        takeAnyInside(this.a) // box int
    }

    fun takeAnyInside(a: Any) {}
}

fun takeNullable(a: UInt?) {}

// 2 INVOKESTATIC UInt\.box
// 0 INVOKEVIRTUAL Foo.unbox

// 1 valueOf
// 0 intValue