// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
package second

@Target(AnnotationTarget.TYPE)
annotation class Anno(val int: Int)

interface Base
fun bar(): Base {}

const val constant = 0

class My<caret>Class: @Anno(constant) Base by bar() {
    @Target(AnnotationTarget.TYPE)
    annotation class Anno(val string: String)
}
