// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
// GROUPING_RULES: org.jetbrains.jet.plugin.findUsages.KotlinDeclarationGroupRuleProvider$KotlinDeclarationGroupingRule
// OPTIONS: usages, constructorUsages
package server

open class <caret>Server {
    class object {
        val NAME = "Server"
    }

    open fun work() {
        println("Server")
    }
}