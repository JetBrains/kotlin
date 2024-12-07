// WITH_STDLIB

fun box(): String {
    val x: String
    run {
        x = "OK"
        val y = x
    }
    return x
}

// 2 ObjectRef
// 1 INNERCLASS kotlin.jvm.internal.Ref\$ObjectRef kotlin.jvm.internal.Ref ObjectRef
