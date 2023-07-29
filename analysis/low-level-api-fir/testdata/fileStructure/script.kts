/* RootScriptStructureElement */class Builder {/* NonReanalyzableClassDeclarationStructureElement */
    var version: String = ""/* NonReanalyzableNonClassDeclarationStructureElement */

    fun execute() {/* ReanalyzableFunctionStructureElement */
        println(version)
    }
}

fun build(action: Builder.() -> Unit) = Builder().apply(action)/* NonReanalyzableNonClassDeclarationStructureElement */
fun build2(action: Builder.() -> Unit): Builder = Builder().apply(action)/* ReanalyzableFunctionStructureElement */

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
}/* NonReanalyzableNonClassDeclarationStructureElement */

builder.execute()
