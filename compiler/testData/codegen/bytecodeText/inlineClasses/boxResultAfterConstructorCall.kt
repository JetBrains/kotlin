// !LANGUAGE: +InlineClasses

inline class AsInt(val value: Int)
inline class AsAny(val value: Any)

fun takeAny(a: Any) {}

fun test() {
    takeAny(AsInt(123)) // box
    takeAny(AsAny(123)) // box int, box inline class
}

// 1 INVOKESTATIC AsInt\$Erased.box
// 0 INVOKEVIRTUAL AsInt.unbox

// 1 INVOKESTATIC AsAny\$Erased.box
// 0 INVOKEVIRTUAL AsAny.unbox

// 1 valueOf
// 0 intValue