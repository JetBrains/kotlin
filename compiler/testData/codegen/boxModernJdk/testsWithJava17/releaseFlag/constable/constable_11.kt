// RELEASE: 11
// CHECK_BYTECODE_TEXT
// 0 CHECKCAST java/lang/constant/Constable
// 0 LOCALVARIABLE constable Ljava/lang/constant/Constable;

import java.lang.invoke.*

fun cond() = true

fun test(mh: MethodHandle?, mt: MethodType?) {
    val z = if (cond()) mh else mt
}

fun box(): String {
    test(null, null)
    return "OK"
}