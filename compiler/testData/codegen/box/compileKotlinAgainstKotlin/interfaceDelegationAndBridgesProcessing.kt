// MODULE: lib
// FILE: A.kt

package test

interface CodeBlock {
    fun foo(): String
}

interface CompositeCodeBlock: CodeBlock {
    override fun foo(): String {
        return "OK"
    }
}

interface ForLoopBody : CodeBlock

abstract class CodeBlockBase: CompositeCodeBlock

abstract class LineSeparatedCodeBlock: CodeBlockBase()

// MODULE: main(lib)
// FILE: B.kt

import test.*

open class KotlinCodeBlock: LineSeparatedCodeBlock()

class KotlinForLoopBody : KotlinCodeBlock(), ForLoopBody

fun box(): String {
    return KotlinForLoopBody().foo()
}
