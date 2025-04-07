// WITH_STDLIB

fun box(): String {
    val x: String
    val y: Any
    run {
        x = "OK"
        y = Any()
        val a = x
        val b = y
    }
    return x
}

// 2 ObjectRef
// 1 INNERCLASS kotlin.jvm.internal.Ref\$ObjectRef kotlin.jvm.internal.Ref ObjectRef
// 1 LOCALVARIABLE x Ljava/lang/String;
// 1 INNERCLASS kotlin.jvm.internal.Ref\$StringRef kotlin.jvm.internal.Ref StringRef
