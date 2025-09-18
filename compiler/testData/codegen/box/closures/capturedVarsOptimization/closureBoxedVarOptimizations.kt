// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// WITH_STDLIB
// ^ Because kotlin.jvm.JvmInline is not in the minimized stdlib for some reason

// FILECHECK_STAGE: CStubs

// EXPECT_GENERATED_JS: function=captureVarInInlineLambda;captureVarInLocalClassInInlineLambda;captureValueClassVar;captureValueClassVar$lambda expect=closureBoxedVarOptimizations.js TARGET_BACKENDS=JS_IR
// EXPECT_GENERATED_JS: function=captureVarInInlineLambda;captureVarInLocalClassInInlineLambda;captureValueClassVar;captureValueClassVar$lambda expect=closureBoxedVarOptimizations.es6.js TARGET_BACKENDS=JS_IR_ES6

// CHECK_BYTECODE_TEXT
// NO_CHECK_LAMBDA_INLINING
// WASM_FAILS_IN: NodeJs

// FILE: lib.kt
inline fun run(f: () -> Unit) {
    f()
}

// FILE: main.kt
fun run2(f: () -> Unit) {
    f()
}

// CHECK-LABEL: define void @"kfun:#captureVarInInlineLambda(){}"
// CHECK-NOT: call void @"kfun:kotlin.internal.SharedVariableBox#<init>(1:0){}"
// CHECK-NOT: call void @"kfun:kotlin.internal.SharedVariableBoxByte#<init>(kotlin.Byte){}"
// CHECK-NOT: call void @"kfun:kotlin.internal.SharedVariableBoxShort#<init>(kotlin.Short){}"
// CHECK-NOT: call void @"kfun:kotlin.internal.SharedVariableBoxInt#<init>(kotlin.Int){}"
// CHECK-NOT: call void @"kfun:kotlin.internal.SharedVariableBoxLong#<init>(kotlin.Long){}"
// CHECK-NOT: call void @"kfun:kotlin.internal.SharedVariableBoxFloat#<init>(kotlin.Float){}"
// CHECK-NOT: call void @"kfun:kotlin.internal.SharedVariableBoxDouble#<init>(kotlin.Double){}"
// CHECK-NOT: call void @"kfun:kotlin.internal.SharedVariableBoxChar#<init>(kotlin.Char){}"
// CHECK-NOT: call void @"kfun:kotlin.internal.SharedVariableBoxBoolean#<init>(kotlin.Boolean){}"
fun captureVarInInlineLambda() {
    var any: Any? = Any()
    var byte = 1.toByte()
    var short = 2.toShort()
    var int = 3
    var long = 4L
    var float = 5.0f
    var double = 6.0
    var char = 'a'
    var boolean = true
    run {
        any = null
        byte = 101.toByte()
        short = 102.toShort()
        int = 103
        long = 104L
        float = 105.0f
        double = 106.0
        char = 'b'
        boolean = false
    }
}

// CHECK-LABEL: define void @"kfun:#captureVarInLocalClassInInlineLambda(){}"
fun captureVarInLocalClassInInlineLambda() {
    // CHECK: call void @"kfun:kotlin.internal.SharedVariableBox#<init>(1:0){}"
    var any: Any? = Any()

    // CHECK: call void @"kfun:kotlin.internal.SharedVariableBoxByte#<init>(kotlin.Byte){}"
    var byte = 1.toByte()

    // CHECK: call void @"kfun:kotlin.internal.SharedVariableBoxShort#<init>(kotlin.Short){}"
    var short = 2.toShort()

    // CHECK: call void @"kfun:kotlin.internal.SharedVariableBoxInt#<init>(kotlin.Int){}"
    var int = 3

    // CHECK: call void @"kfun:kotlin.internal.SharedVariableBoxLong#<init>(kotlin.Long){}"
    var long = 4L

    // CHECK: call void @"kfun:kotlin.internal.SharedVariableBoxFloat#<init>(kotlin.Float){}"
    var float = 5.0f

    // CHECK: call void @"kfun:kotlin.internal.SharedVariableBoxDouble#<init>(kotlin.Double){}"
    var double = 6.0

    // CHECK: call void @"kfun:kotlin.internal.SharedVariableBoxChar#<init>(kotlin.Char){}"
    var char = 'a'

    // CHECK: call void @"kfun:kotlin.internal.SharedVariableBoxBoolean#<init>(kotlin.Boolean){}"
    var boolean = true
    run {
        object {
            fun foo() {
                any = null
                byte = 101.toByte()
                short = 102.toShort()
                int = 103
                long = 104L
                float = 105.0f
                double = 106.0
                char = 'b'
                boolean = false
            }
        }.foo()
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class AnyWrapper(val v: Any?)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ByteWrapper(val v: Byte)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ShortWrapper(val v: Short)

OPTIONAL_JVM_INLINE_ANNOTATION
value class IntWrapper(val v: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class LongWrapper(val v: Long)

OPTIONAL_JVM_INLINE_ANNOTATION
value class FloatWrapper(val v: Float)

OPTIONAL_JVM_INLINE_ANNOTATION
value class DoubleWrapper(val v: Double)

OPTIONAL_JVM_INLINE_ANNOTATION
value class CharWrapper(val v: Char)

OPTIONAL_JVM_INLINE_ANNOTATION
value class BooleanWrapper(val v: Boolean)

// CHECK-LABEL: define void @"kfun:#captureValueClassVar(){}"
fun captureValueClassVar() {
    // CHECK: call ptr @"kfun:#<AnyWrapper-box>(AnyWrapper){}kotlin.Any"
    // CHECK: call void @"kfun:kotlin.internal.SharedVariableBox#<init>(1:0){}"
    var any = AnyWrapper(Any())

    // CHECK: call ptr @"kfun:#<ByteWrapper-box>(ByteWrapper){}kotlin.Any"
    // CHECK: call void @"kfun:kotlin.internal.SharedVariableBox#<init>(1:0){}"
    var byte = ByteWrapper(1.toByte())

    // CHECK: call ptr @"kfun:#<ShortWrapper-box>(ShortWrapper){}kotlin.Any"
    // CHECK: call void @"kfun:kotlin.internal.SharedVariableBox#<init>(1:0){}"
    var short = ShortWrapper(2.toShort())

    // CHECK: call ptr @"kfun:#<IntWrapper-box>(IntWrapper){}kotlin.Any"
    // CHECK: call void @"kfun:kotlin.internal.SharedVariableBox#<init>(1:0){}"
    var int = IntWrapper(3)

    // CHECK: call ptr @"kfun:#<LongWrapper-box>(LongWrapper){}kotlin.Any"
    // CHECK: call void @"kfun:kotlin.internal.SharedVariableBox#<init>(1:0){}"
    var long = LongWrapper(4L)

    // CHECK: call ptr @"kfun:#<FloatWrapper-box>(FloatWrapper){}kotlin.Any"
    // CHECK: call void @"kfun:kotlin.internal.SharedVariableBox#<init>(1:0){}"
    var float = FloatWrapper(5.0f)

    // CHECK: call ptr @"kfun:#<DoubleWrapper-box>(DoubleWrapper){}kotlin.Any"
    // CHECK: call void @"kfun:kotlin.internal.SharedVariableBox#<init>(1:0){}"
    var double = DoubleWrapper(6.0)

    // CHECK: call ptr @"kfun:#<CharWrapper-box>(CharWrapper){}kotlin.Any"
    // CHECK: call void @"kfun:kotlin.internal.SharedVariableBox#<init>(1:0){}"
    var char = CharWrapper('a')

    // CHECK: call ptr @"kfun:#<BooleanWrapper-box>(BooleanWrapper){}kotlin.Any"
    // CHECK: call void @"kfun:kotlin.internal.SharedVariableBox#<init>(1:0){}"
    var boolean = BooleanWrapper(true)
    run2 {
         any = AnyWrapper(null)
         byte = ByteWrapper(101.toByte())
         short = ShortWrapper(102.toShort())
         int = IntWrapper(103)
         long = LongWrapper(104L)
         float = FloatWrapper(105.0f)
         double = DoubleWrapper(106.0)
         char = CharWrapper('b')
         boolean = BooleanWrapper(false)
    }
}

fun box(): String {
    captureVarInInlineLambda()
    captureVarInLocalClassInInlineLambda()
    captureValueClassVar()
    return "OK"
}

// 2 INVOKESPECIAL kotlin/jvm/internal/Ref\$ObjectRef\.<init> \(\)V
// 2 INVOKESPECIAL kotlin/jvm/internal/Ref\$ByteRef\.<init> \(\)V
// 2 INVOKESPECIAL kotlin/jvm/internal/Ref\$ShortRef\.<init> \(\)V
// 2 INVOKESPECIAL kotlin/jvm/internal/Ref\$IntRef\.<init> \(\)V
// 2 INVOKESPECIAL kotlin/jvm/internal/Ref\$LongRef\.<init> \(\)V
// 2 INVOKESPECIAL kotlin/jvm/internal/Ref\$FloatRef\.<init> \(\)V
// 2 INVOKESPECIAL kotlin/jvm/internal/Ref\$DoubleRef\.<init> \(\)V
// 2 INVOKESPECIAL kotlin/jvm/internal/Ref\$CharRef\.<init> \(\)V
// 2 INVOKESPECIAL kotlin/jvm/internal/Ref\$BooleanRef\.<init> \(\)V
// 1 NEW AnyWrapper
// 1 NEW ByteWrapper
// 1 NEW ShortWrapper
// 1 NEW IntWrapper
// 1 NEW LongWrapper
// 1 NEW FloatWrapper
// 1 NEW DoubleWrapper
// 1 NEW CharWrapper
// 1 NEW BooleanWrapper
