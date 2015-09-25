//ALLOW_AST_ACCESS
package test

const private val topLevel = 1

object A {
    const protected val inObject = 2
}

class B {
    companion object {
        const val inCompanion = 3
    }
}
