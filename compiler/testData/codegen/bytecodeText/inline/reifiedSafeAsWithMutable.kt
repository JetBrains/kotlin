// For mutable collections and related types (e.g., MutableList, MutableListIterator)
// 'as?' should be generated as a single 'safeAs...' intrinsic call
// without instanceof or 'is...'.

inline fun <reified T> safeAs(x: Any): T? {
    return x as? T
}

fun test() {
    val x: Any = arrayListOf("abc", "def")
    safeAs<MutableList<*>>(x)?.clear()
}

// 0 INSTANCEOF java/util/List
// 1 INVOKESTATIC kotlin/jvm/internal/TypeIntrinsics\.isMutableList
// 0 INVOKESTATIC kotlin/jvm/internal/TypeIntrinsics\.safeAsMutableList
// 1 CHECKCAST java/util/List