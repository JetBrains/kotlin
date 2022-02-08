// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory
// 1 class InheritedWithCharDiamondKt\$box\$1

fun interface GenericToAny<T> {
    fun invoke(x: T): Any
}

fun interface CharToAny {
    fun invoke(x: Char): Any
}

fun interface GenericCharToAny : GenericToAny<Char>, CharToAny

fun withK(fn: GenericCharToAny) = fn.invoke('K').toString()

fun box(): String =
    withK { "O" + it }