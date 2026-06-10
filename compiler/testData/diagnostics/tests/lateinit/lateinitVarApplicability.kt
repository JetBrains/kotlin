// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
object Delegate {
    operator fun getValue(instance: Any?, property: Any) : String = ""
    operator fun setValue(instance: Any?, property: Any, value: String) {}
}

<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var test: Int
<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var kest by Delegate

lateinit var good: String

class A {
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val fest = "10"
    lateinit var mest: String
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var xest: String?
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var nest: Int
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var west: Char
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var qest: Boolean
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var aest: Short
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var hest: Byte
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var jest: Long
    <!INAPPLICABLE_LATEINIT_MODIFIER, INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val dest: String
        get() = "KEKER"
}

class B<T> {
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var best: T
}

class C<K : Any> {
    lateinit var pest: K
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var vest: K?
}

abstract class Abstract {
    abstract <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var foo: String
}

open class Base {
    open lateinit var foo: String
    open lateinit var bar: String
    open var baz: String
        get() = ""
        set(value) {}
}

class Derived : Base() {
    override lateinit var foo: String
    override var bar: String = ""
    override lateinit var baz: String
}

fun rest() {
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var i: Int
    lateinit var a: A
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var b: B<String> = B()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, lateinit, localProperty, nullableType,
objectDeclaration, operator, propertyDeclaration, propertyDelegate, setter, stringLiteral, typeConstraint, typeParameter */
