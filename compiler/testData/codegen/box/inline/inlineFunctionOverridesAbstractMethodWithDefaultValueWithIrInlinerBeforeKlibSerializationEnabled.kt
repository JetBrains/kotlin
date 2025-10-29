// ISSUE: KT-82017
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// IGNORE_IR_DESERIALIZATION_TEST: ANY
// IGNORE_BACKEND: ANY
// ^^^ KT-82017 Incomplete expression: call to FUN name:foo visibility:public modality:OPEN <> (<this>:<root>.A, a:kotlin.Int) returnType:kotlin.Int [inline] has no argument at index 1

interface I {
    abstract fun foo(a: String = "FAIL"): String
}

class A(): I {
    inline override fun foo(a: String): String = "OK"
}

fun box() = A().foo()