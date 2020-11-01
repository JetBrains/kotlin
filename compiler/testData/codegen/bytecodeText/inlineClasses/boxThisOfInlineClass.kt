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

// 1 valueOf
// 0 intValue

// -- 1 before takeNullable
// -- 1 before takeAnyInside
// 2 INVOKESTATIC UInt\.box

// JVM_TEMPLATES:
// -- equals-impl
// 1 INVOKEVIRTUAL UInt\.unbox

// JVM_IR_TEMPLATES:
// -- getA, toString, hashCode, equals-impl, equals
// 5 INVOKEVIRTUAL UInt\.unbox