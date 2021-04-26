// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 3 java/lang/invoke/LambdaMetafactory

fun interface IFoo {
    fun foo(k: String): String
}

fun fooK(iFoo: IFoo) = iFoo.foo("K")

fun box() =
    fooK {
        fooK {
            fooK { k -> "O" + k }
        }
    }