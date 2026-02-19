// ISSUE: KT-70854
// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol

interface Base {
    fun print()
}

class BaseImpl(val x: Int) : Base {
    override fun print() {}
}

@Target(AnnotationTarget.EXPRESSION)
annotation class Some(val s: String)

class Der<caret>ived(b: Base) : Base by @Some("Anything") b
