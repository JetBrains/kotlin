// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtNameReferenceExpression
package foo

annotation class Anno(val i: Int)

const val CONSTANT = 1

enum class MyEnumClass {
    Entry {
        @Anno( <expr>CONSTANT</expr> )
    }
}
