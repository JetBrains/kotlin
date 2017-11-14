// For mutable collections and related types (e.g., MutableList, MutableListIterator)
// 'as?' should be generated as a single 'safeAs...' intrinsic call
// without instanceof or 'is...'.

val x: Any = arrayListOf("abc", "def")

fun test() = x as? MutableList<*>

// 0 INSTANCEOF
// 1 INVOKESTATIC kotlin/jvm/internal/TypeIntrinsics\.isMutableList
// 0 INVOKESTATIC kotlin/jvm/internal/TypeIntrinsics\.asMutableList
// 1 CHECKCAST java/util/List