fun foo() {
    val x = 1
    fun bar()  {
        val y = x
    }
}

// Local function bodies are in a separate class (implementing FunctionN) for non-IR, and are static methods in the enclosing class for IR.

// JVM_TEMPLATES
// METHOD : LocalFunKt$foo$1.invoke()V
// VARIABLE : NAME=y TYPE=I INDEX=1
// VARIABLE : NAME=this TYPE=LLocalFunKt$foo$1; INDEX=0

// JVM_IR_TEMPLATES
// METHOD : LocalFunKt.foo$bar(I)V
// VARIABLE : NAME=y TYPE=I INDEX=1
// VARIABLE : NAME=$x TYPE=I INDEX=0
