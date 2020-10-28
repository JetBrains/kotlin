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

// JVM_TEMPLATES:
// 2 INVOKESTATIC UInt\.box
// -- equals-impl
// 1 INVOKEVIRTUAL UInt\.unbox

// JVM_IR_TEMPLATES:
// -- 1 before takeNullable
// -- 1 before takeAnyInside
// -- 1 before takeAnyInside's GETFIELD
// 3 INVOKESTATIC UInt\.box
// -- getA, toString, hashCode, equals-impl, equals
// 5 INVOKEVIRTUAL UInt\.unbox