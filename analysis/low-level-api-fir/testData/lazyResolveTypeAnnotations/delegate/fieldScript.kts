// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
package one

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

class Usa<caret>ge(prop: List<List<Int>>) : @Anno("super type ref $prop") List<@Anno("nested super type ref $prop") List<@Anno("nested nested super type ref $prop") Int>> by prop
