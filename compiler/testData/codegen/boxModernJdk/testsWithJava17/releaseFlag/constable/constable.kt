// CHECK_BYTECODE_TEXT
// 2 CHECKCAST java/lang/constant/Constable
// 1 LOCALVARIABLE constable Ljava/lang/constant/Constable;

import java.lang.invoke.*

fun cond() = true

fun test(mh: MethodHandle?, mt: MethodType?) {
    val constable = if (cond()) mh else mt
}

fun box(): String {
    test(null, null)
    return "OK"
}