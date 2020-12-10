// COMPILER_ARGUMENTS: -XXLanguage:+SealedInterfaces -XXLanguage:+FreedomForSealedClasses
package sealed

class C: SealedDeclarationInterface {}
class CClass: SealedDeclarationClass() {}

class D: SealedDeclarationInterface {
    class E: SealedDeclarationInterface {
        class F: SealedDeclarationInterface
    }
}

class DClass: SealedDeclarationClass() {
    class EClass: SealedDeclarationClass() {
        class FClass: SealedDeclarationClass()
    }
}

fun checkWhenNone(value: SealedDeclarationInterface): Int = <error descr="[NO_ELSE_IN_WHEN] 'when' expression must be exhaustive, add necessary 'is A', 'is B', 'is C', 'is D', 'is E', 'is F' branches or 'else' branch instead">when</error> (<warning descr="[UNUSED_EXPRESSION] The expression is unused">value</warning>) {
}

fun checkWhenNone(value: SealedDeclarationClass): Int = <error descr="[NO_ELSE_IN_WHEN] 'when' expression must be exhaustive, add necessary 'is AClass', 'is BClass', 'is CClass', 'is DClass', 'is EClass', 'is FClass' branches or 'else' branch instead">when</error> (<warning descr="[UNUSED_EXPRESSION] The expression is unused">value</warning>) {
}

fun checkWhenOneMissing(value: SealedDeclarationInterface): Int = <error descr="[NO_ELSE_IN_WHEN] 'when' expression must be exhaustive, add necessary 'is D', 'is E', 'is F' branches or 'else' branch instead">when</error> (value) {
    is SealedDeclarationInterface.A -> 1
    is B -> 2
    is C -> 3
}

fun checkWhenOneMissing(value: SealedDeclarationClass): Int = <error descr="[NO_ELSE_IN_WHEN] 'when' expression must be exhaustive, add necessary 'is DClass', 'is EClass', 'is FClass' branches or 'else' branch instead">when</error> (value) {
    is SealedDeclarationClass.AClass -> 1
    is BClass -> 2
    is CClass -> 3
}