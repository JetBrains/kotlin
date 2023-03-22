// IGNORE_REVERSED_RESOLVE
// !LANGUAGE: +LateinitTopLevelProperties +LateinitLocalVariables
import kotlin.reflect.KProperty

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: String) {}
}

public abstract class A<T: Any, V: String?>(<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var p2: String) {

    public <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val a: String
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val b: T
    private lateinit var c: CharSequence

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val d: String
        get

    public lateinit var e: String
        get
        private set

    fun a() {
        lateinit var a: String
    }

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e1: V
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e2: String?
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e3: Int
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e4: Int?
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e5 = "A"

    // With initializer, primitive
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e6 = 3

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e7 by CustomDelegate()

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e8: String
        get() = "A"

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e9: String
        set(v) { field = v }

    abstract <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e10: String

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var String.e11: String

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var String.e12: String
}

<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val topLevel: String
lateinit var topLevelMutable: String

public interface Intf {
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var str: String
}

public abstract class AbstractClass {
    abstract var str: String
}

public class AbstractClassImpl : AbstractClass() {
    override lateinit var str: String
}

public class B {
    lateinit var a: String

    init {
        a.length
    }
}
