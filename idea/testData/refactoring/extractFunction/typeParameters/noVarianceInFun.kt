// PARAM_DESCRIPTOR: val e: T? defined in EntityClass.foo
// PARAM_TYPES: T?, Entity?
// SIBLING:
abstract public class EntityClass<out T: Entity>() {
    fun foo() {
        val e: T? = null
        <selection>e?.value</selection>
    }
}

open class Entity {
    var value: String? = null
}