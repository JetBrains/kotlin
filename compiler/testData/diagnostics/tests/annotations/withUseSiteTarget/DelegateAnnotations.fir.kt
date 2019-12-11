import kotlin.reflect.KProperty

@Target(AnnotationTarget.FIELD) annotation class Field

@Target(AnnotationTarget.PROPERTY) annotation class Prop

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

@delegate:Field
class SomeClass {

    @delegate:Field
    constructor()

    @delegate:Field @delegate:Prop
    protected val simpleProperty: String = "text"

    @delegate:Field @delegate:Prop
    protected val delegatedProperty: String by CustomDelegate()

    @delegate:Field @delegate:Prop
    val propertyWithCustomGetter: Int
        get() = 5

}

class WithPrimaryConstructor(@delegate:Field @delegate:Prop val a: String,
                             @param:Field @param:Prop val b: String)

fun foo(@delegate:Field @delegate:Prop x: Int) = x

