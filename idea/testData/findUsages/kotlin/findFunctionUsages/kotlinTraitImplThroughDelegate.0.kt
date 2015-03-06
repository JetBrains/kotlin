// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages, skipImports
package server

trait TraitWithImpl {
    fun <caret>foo() = 1
}

public class TraitWithDelegatedWithImpl(f: TraitWithImpl): TraitWithImpl by f

fun test(twdwi: TraitWithDelegatedWithImpl) = twdwi.foo()