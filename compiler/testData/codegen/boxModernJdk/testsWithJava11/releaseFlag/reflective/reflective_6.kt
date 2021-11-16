// JDK_RELEASE: 6
// CHECK_BYTECODE_TEXT
// 0 CHECKCAST java/lang/ReflectiveOperationException
// 0 LOCALVARIABLE reflective Ljava/lang/ReflectiveOperationException;

fun cond() = true

fun test(iae: IllegalAccessException?, cnfe: ClassNotFoundException?) {
    val reflective = if (cond()) iae else cnfe
}

fun box(): String {
    test(null, null)
    return "OK"
}