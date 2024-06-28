// IGNORE_TREE_ACCESS: KT-64898
import kotlin.reflect.KProperty

annotation class Ann

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

@field:Ann
class SomeClass {

    @field:Ann
    constructor()

    @field:Ann
    protected val simpleProperty: String = "text"

    @field:[Ann]
    protected val simplePropertyWithAnnotationList: String = "text"

    @field:Ann
    protected val delegatedProperty: String by CustomDelegate()

    @field:Ann
    val propertyWithCustomGetter: Int
        get() = 5

    @field:Ann
    fun anotherFun(@field:Ann s: String) {
        @field:Ann
        val localVariable = 5
    }

}

class WithPrimaryConstructor(@field:Ann val a: String)
