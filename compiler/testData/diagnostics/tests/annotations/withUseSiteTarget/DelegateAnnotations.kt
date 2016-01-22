import kotlin.reflect.KProperty

@Target(AnnotationTarget.FIELD) annotation class Field

@Target(AnnotationTarget.PROPERTY) annotation class Prop

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@delegate:Field<!>
class SomeClass {

    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@delegate:Field<!>
    constructor()

    <!INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE!>@delegate:Field<!> <!INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@delegate:Prop<!>
    protected val simpleProperty: String = "text"

    @delegate:Field <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@delegate:Prop<!>
    protected val delegatedProperty: String by CustomDelegate()

    <!INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@delegate:Field<!> <!INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@delegate:Prop<!>
    val propertyWithCustomGetter: Int
        get() = 5

}

class WithPrimaryConstructor(<!INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE!>@delegate:Field<!> <!INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@delegate:Prop<!> val a: String,
                             <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@param:Field<!> <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@param:Prop<!> val b: String)

fun foo(<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@delegate:Field<!> <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@delegate:Prop<!> x: Int) = x

