fun (a: Int = 1): String = "str"/* NonReanalyzableNonClassDeclarationStructureElement */

fun () {/* NonReanalyzableNonClassDeclarationStructureElement */

}

val : Int = 4/* NonReanalyzableNonClassDeclarationStructureElement */

var : Int/* NonReanalyzableNonClassDeclarationStructureElement */
    get() = 4
    set(value) {

    }

class A {/* NonReanalyzableClassDeclarationStructureElement */
    fun (a: Int = 1): String = "str"/* NonReanalyzableNonClassDeclarationStructureElement */

    fun () {/* NonReanalyzableNonClassDeclarationStructureElement */

    }

    val : Int = 4/* NonReanalyzableNonClassDeclarationStructureElement */

    var : Boolean/* NonReanalyzableNonClassDeclarationStructureElement */
        get() = true
        set(value) {

        }
}
