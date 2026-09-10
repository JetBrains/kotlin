// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// LANGUAGE_FEATURE_TOGGLED: LateinitVals

object Delegate {
    operator fun getValue(instance: Any?, property: Any) : String = ""
    operator fun setValue(instance: Any?, property: Any, value: String) {}
}

lateinit val test: Int
<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val kest by Delegate

lateinit val good: String

class A {
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val fest = "10"
    lateinit val mest: String
    lateinit val xest: String?
    lateinit val nest: Int
    lateinit val west: Char
    lateinit val qest: Boolean
    lateinit val aest: Short
    lateinit val hest: Byte
    lateinit val jest: Long
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val dest: String
        get() = "KEKER"
}

class B<T> {
    lateinit val best: T
}

class C<K : Any> {
    lateinit val pest: K
    lateinit val vest: K?
}

abstract class Abstract {
    abstract <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val foo: String
}

open class Base {
    open lateinit val foo: String
    open lateinit val bar: String
    open val baz: String
        get() = ""
}

class Derived : Base() {
    override lateinit val foo: String
    override <!LATEINIT_VAL_OVERRIDDEN_BY_VAL!>val<!> bar: String = ""
    override lateinit val baz: String
}

fun rest() {
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val i: Int
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val a: A
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val b: B<String> = B()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, lateinit, localProperty, nullableType,
objectDeclaration, operator, propertyDeclaration, propertyDelegate, stringLiteral, typeConstraint, typeParameter */
