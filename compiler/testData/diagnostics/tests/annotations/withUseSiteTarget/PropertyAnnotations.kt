import kotlin.reflect.KProperty

annotation class Ann
annotation class Second

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

<!INAPPLICABLE_TARGET_ON_PROPERTY_WARNING, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@property:Ann<!>
class SomeClass {

    <!INAPPLICABLE_TARGET_ON_PROPERTY_WARNING, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@property:Ann<!>
    constructor(s: String)

    @property:Ann
    protected val p1: String = ""

    @property:[Ann Second]
    protected val p2: String = ""

    @property:Ann
    protected var p3: String = ""

    @property:Ann
    protected val p4: String by CustomDelegate()

    @property:Ann
    var propertyWithCustomSetter: Int
        get() = 5
        set(v) {}

    <!INAPPLICABLE_TARGET_ON_PROPERTY_WARNING, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@property:Ann<!>
    fun anotherFun() {
        <!INAPPLICABLE_TARGET_ON_PROPERTY_WARNING, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@property:Ann<!>
        val localVariable = 5
    }

}
