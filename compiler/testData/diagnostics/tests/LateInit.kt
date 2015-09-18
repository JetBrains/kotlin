class CustomDelegate {
    public fun get(thisRef: Any?, prop: PropertyMetadata): String = prop.name
}

public abstract class A<T: Any, V: String?>(<!INAPPLICABLE_LATEINIT_MODIFIER_PRIMARY_CONSTRUCTOR_PARAMETER!>lateinit<!> var p2: String) {

    public <!INAPPLICABLE_LATEINIT_MODIFIER_IMMUTABLE!>lateinit<!> val a: String
    <!INAPPLICABLE_LATEINIT_MODIFIER_IMMUTABLE!>lateinit<!> val b: T
    private lateinit var c: CharSequence

    <!INAPPLICABLE_LATEINIT_MODIFIER_IMMUTABLE!>lateinit<!> val d: String
        get

    public lateinit var e: String
        get
        private set

    fun a() {
        <!WRONG_MODIFIER_TARGET!>lateinit<!> var <!UNUSED_VARIABLE!>a<!>: String
    }

    <!INAPPLICABLE_LATEINIT_MODIFIER_NULLABLE!>lateinit<!> var e1: V
    <!INAPPLICABLE_LATEINIT_MODIFIER_NULLABLE!>lateinit<!> var e2: String?
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e3: Int
    <!INAPPLICABLE_LATEINIT_MODIFIER_NULLABLE!>lateinit<!> var e4: Int?
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e5 = "A"
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e6 = 3

    <!INAPPLICABLE_LATEINIT_MODIFIER_IMMUTABLE!>lateinit<!> val e7 by CustomDelegate()

    <!INAPPLICABLE_LATEINIT_MODIFIER_IMMUTABLE!>lateinit<!> val e8: String
        get() = "A"

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e9: String
        set(v) { field = v }

    abstract <!INAPPLICABLE_LATEINIT_MODIFIER_ABSTRACT_PROPERTY!>lateinit<!> var e10: String

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var String.e11: String

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var String.e12: String
}

<!INAPPLICABLE_LATEINIT_MODIFIER_IMMUTABLE, WRONG_MODIFIER_TARGET!>lateinit<!> val topLevel: String
<!WRONG_MODIFIER_TARGET!>lateinit<!> var topLevelMutable: String

public interface Intf {
    <!INAPPLICABLE_LATEINIT_MODIFIER_ABSTRACT_PROPERTY!>lateinit<!> var str: String
}

public abstract class AbstractClass {
    abstract var str: String
}

public class AbstractClassImpl : AbstractClass() {
    override lateinit var str: String
}