// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
package second

open class Base

class MyC<caret>lass() : Base() {
    open class Base
}
