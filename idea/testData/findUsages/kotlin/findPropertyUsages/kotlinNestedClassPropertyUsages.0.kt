// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtProperty
// OPTIONS: usages
package a

public open class Outer() {
    open class Inner {
        var <caret>foo: Int = 1
    }
}
