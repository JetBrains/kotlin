// LAMBDAS: CLASS

fun foo() {
    fun bar() : (Int) -> Unit {
        return {
            1
        }
    }
}

// Names of local functions are used in names of local classes for IR, but only indices are used for non-IR.

// JVM_TEMPLATES
// METHOD : ItInReturnedLambdaKt$foo$1$1.invoke(I)V
// VARIABLE : NAME=this TYPE=LItInReturnedLambdaKt$foo$1$1; INDEX=0
// VARIABLE : NAME=it TYPE=I INDEX=1

// JVM_IR_TEMPLATES
// METHOD : ItInReturnedLambdaKt$foo$bar$1.invoke(I)V
// VARIABLE : NAME=this TYPE=LItInReturnedLambdaKt$foo$bar$1; INDEX=0
// VARIABLE : NAME=it TYPE=I INDEX=1
