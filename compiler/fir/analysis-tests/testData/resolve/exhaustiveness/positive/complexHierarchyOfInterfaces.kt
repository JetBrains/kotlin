// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80461

sealed interface Base1
sealed interface Base2
sealed interface Base3
sealed interface Derived3 : Base3
sealed interface DoubleDerived3 : Base3, Derived3

abstract class Impl13 : DoubleDerived3, Base1

abstract class Impl23 : Base2, Derived3
class DerivedImpl23 : Impl23(), Base3

fun foo(x: Base3): String? = <!WHEN_ON_SEALED!>when (x) {
    is Impl13 -> null
    is Base2 -> null
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, isExpression, nullableType, sealed,
smartcast, whenExpression, whenWithSubject */
