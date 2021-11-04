// RELEASE: 11
// CHECK_BYTECODE_TEXT
// 2 CHECKCAST java/lang/ReflectiveOperationException
// 1 LOCALVARIABLE reflective Ljava/lang/ReflectiveOperationException;

fun cond() = true

fun test(iae: IllegalAccessException?, cnfe: ClassNotFoundException?) {
    val reflective = if (cond()) iae else cnfe
}

fun box(): String {
    test(null, null)
    return "OK"
}