// !LANGUAGE: +InlineClasses

inline class UInt(val value: Int)

fun test(x: UInt?) {
    x?.myLet { // unbox
        takeUInt(it)
        takeUInt(x) // unbox
    }

    x?.myLet { // unbox
        takeNullableUInt(it) // box
        takeNullableUInt(x)
    }

    x!!.myLet { // unbox
        takeUInt(it)
        takeUInt(x) // unbox
    }
}

fun takeUInt(y: UInt) {}
fun takeNullableUInt(y: UInt?) {}

inline fun <T> T.myLet(f: (T) -> Unit) = f(this)

// 1 INVOKESTATIC UInt\$Erased.box
// 5 INVOKEVIRTUAL UInt.unbox

// 0 intValue
// 0 valueOf