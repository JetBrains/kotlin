/* RootScriptStructureElement */fun (a: Int = 1): String = "str"/* ReanalyzableFunctionStructureElement */

fun () {/* ReanalyzableFunctionStructureElement */

}

val : Int = 4/* NonReanalyzableNonClassDeclarationStructureElement */

var : Int/* ReanalyzablePropertyStructureElement */
    get() = 4
    set(value) {

    }

class A {/* NonReanalyzableClassDeclarationStructureElement */
    fun (a: Int = 1): String = "str"/* ReanalyzableFunctionStructureElement */

    fun () {/* ReanalyzableFunctionStructureElement */

    }

    val : Int = 4/* NonReanalyzableNonClassDeclarationStructureElement */

    var : Boolean/* ReanalyzablePropertyStructureElement */
        get() = true
        set(value) {

        }
}
