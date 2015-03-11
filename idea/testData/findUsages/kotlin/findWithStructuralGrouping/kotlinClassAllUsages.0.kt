// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// GROUPING_RULES: org.jetbrains.kotlin.idea.findUsages.KotlinDeclarationGroupRuleProvider$KotlinDeclarationGroupingRule
// OPTIONS: usages, constructorUsages
package server

open class <caret>Server {
    default object {
        val NAME = "Server"
    }

    open fun work() {
        println("Server")
    }
}
