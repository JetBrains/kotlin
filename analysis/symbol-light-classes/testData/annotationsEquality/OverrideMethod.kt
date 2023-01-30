// PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod
// EXPECTED: java.lang.Override

package one

abstract class AbstractClass {
    abstract fun foo()
}

class OverrideMethod : AbstractClass() {
    override fun f<caret>oo() {
    }
}
