// COMPILER_ARGUMENTS: -XXLanguage:+SealedInterfaces -XXLanguage:+FreedomForSealedClasses

package sealed

sealed class SealedDeclarationClass {
    class AClass: SealedDeclarationClass()
}

sealed interface SealedDeclarationInterface {
    class A: SealedDeclarationInterface
}

class B: SealedDeclarationInterface
class BClass: SealedDeclarationClass()