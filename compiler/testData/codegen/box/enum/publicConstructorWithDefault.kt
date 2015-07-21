// KT-8438 org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException: Error at instruction 4: Cannot pop operand off an empty stack

enum class E public constructor(x: String = "OK") {
    ENTRY();

    val result = x
}

fun box(): String = E.ENTRY.result
