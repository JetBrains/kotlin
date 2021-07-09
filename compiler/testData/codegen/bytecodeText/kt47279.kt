// TARGET_BACKEND: JVM_IR

fun box() {
    null?.run { return }
}

// 1 private final static box\$lambda-0\(Ljava.lang.Void;\)V