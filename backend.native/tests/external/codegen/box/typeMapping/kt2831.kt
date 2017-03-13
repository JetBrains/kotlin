// Exception in thread "main" java.lang.VerifyError: (class: org/jetbrains/kannotator/controlFlowBuilder/GraphBuilderInterpreter, method: binaryOperation signature: (Lorg/objectweb/asm/tree/AbstractInsnNode;Lorg/jetbrains/kannotator/controlFlowBuilder/AsmPossibleValues;Lorg/jetbrains/kannotator/controlFlowBuilder/AsmPossibleValues;)Lorg/jetbrains/kannotator/controlFlowBuilder/AsmPossibleValues;) Wrong return type in function

fun foo(): Nothing = throw Exception()

fun bar(x: Any): Int {
  return when(x) {
    is String -> 0
    is Int -> 1
    else -> foo()
  }
}

fun box(): String {
    bar(3)
    return "OK"
}