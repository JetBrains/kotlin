// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: usages, constructorUsages
fun test() {
    val kk: KK = <caret>KK()
}

class KK internal constructor()