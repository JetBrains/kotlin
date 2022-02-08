// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory
// 1 final synthetic class BoundReferenceKt\$box\$[0-9]*

fun interface KRunnable {
    fun run()
}

fun runIt(kr: KRunnable) {
    kr.run()
}

class C(var value: String) {
    fun fn() {
        value = "OK"
    }
}

fun box(): String {
    val c = C("xxx")
    runIt(c::fn)
    return c.value
}