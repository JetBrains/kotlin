// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
// BODY_RESOLVE
package util

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

interface I

class <caret>MyClass() : @Anno("super type ref $prop") List<@Anno("nested super type ref $prop") List<@Anno("nested nested super type ref $prop") I>>