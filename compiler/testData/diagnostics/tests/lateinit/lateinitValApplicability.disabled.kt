// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// LANGUAGE_FEATURE_TOGGLED: LateinitVals

object Delegate {
    operator fun getValue(instance: Any?, property: Any) : String = ""
    operator fun setValue(instance: Any?, property: Any, value: String) {}
}

<!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val test: Int
<!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val kest by Delegate

<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val good: String

class A {
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val fest = "10"
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val mest: String
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val xest: String?
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val nest: Int
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val west: Char
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val qest: Boolean
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val aest: Short
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val hest: Byte
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val jest: Long
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val dest: String
        get() = "KEKER"
}

class B<T> {
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val best: T
}

class C<K : Any> {
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val pest: K
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val vest: K?
}

abstract class Abstract {
    abstract <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val foo: String
}

open class Base {
    open <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val foo: String
    open <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val bar: String
    open val baz: String
        get() = ""
}

class Derived : Base() {
    override <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val foo: String
    override <!LATEINIT_VAL_OVERRIDDEN_BY_VAL!>val<!> bar: String = ""
    override <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val baz: String
}

fun rest() {
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val i: Int
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val a: A
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val b: B<String> = B()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, lateinit, localProperty, nullableType,
objectDeclaration, operator, propertyDeclaration, propertyDelegate, stringLiteral, typeConstraint, typeParameter */
