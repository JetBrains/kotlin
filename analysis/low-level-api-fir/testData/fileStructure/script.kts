/* RootScriptStructureElement */class Builder {/* ClassDeclarationStructureElement */
    var version: String = ""/* DeclarationStructureElement */

    fun execute() {/* DeclarationStructureElement */
        println(version)
    }
}

fun build(action: Builder.() -> Unit) = Builder().apply(action)/* DeclarationStructureElement */
fun build2(action: Builder.() -> Unit): Builder = Builder().apply(action)/* DeclarationStructureElement */

build {
    version = "123"
    class A {
        fun doo() {

        }
    }

    execute()
}

val builder = build {
    version = "321"
}/* DeclarationStructureElement */

builder.execute()
