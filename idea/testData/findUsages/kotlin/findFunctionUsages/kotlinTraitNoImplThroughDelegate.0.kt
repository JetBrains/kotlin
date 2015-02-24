// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages, skipImports
package server

trait TraitNoImpl {
    fun <caret>foo()
}

public class TraitWithDelegatedNoImpl(f: TraitNoImpl): TraitNoImpl by f

fun test(twdni: TraitWithDelegatedNoImpl) = twdni.foo()