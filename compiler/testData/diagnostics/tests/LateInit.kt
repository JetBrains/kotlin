class CustomDelegate {
    public fun get(thisRef: Any?, prop: PropertyMetadata): String = prop.name
}

public abstract class A<T: Any, V: String?>(<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val p2: String) {

    public lateinit val a: String
    lateinit val b: T
    private lateinit var c: CharSequence

    lateinit val d: String
        get

    public lateinit var e: String
        get
        private set

    fun a() {
        <!WRONG_MODIFIER_TARGET!>lateinit<!> val <!UNUSED_VARIABLE!>a<!>: String
    }

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val e1: V
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val e2: String?
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val e3: Int
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val e4: Int?
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e5 = "A"
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e6 = 3

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val e7 by CustomDelegate()

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val e8: String
        get() = "A"

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e9: String
        set(v) { $e9 = v }

    abstract <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val e10: String

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val String.e11: String

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var String.e12: String
}

<!WRONG_MODIFIER_TARGET!>lateinit<!> val topLevel: String
<!WRONG_MODIFIER_TARGET!>lateinit<!> var topLevelMutable: String

public interface Intf {
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val str: String
}

public abstract class AbstractClass {
    abstract val str: String
}

public class AbstractClassImpl : AbstractClass() {
    override lateinit val str: String
}