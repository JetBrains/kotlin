// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

<!UNSUPPORTED!>context(_: String)<!>
class C {
    <!UNSUPPORTED!>context(_: String)<!>
    constructor() {}
}

class C2 {
    <!UNSUPPORTED!>context(_: String)<!>
    constructor() {}

    <!UNSUPPORTED!>context(_: String)<!>
    init {
    }
}

<!UNSUPPORTED!>context(_: String)<!>
interface I

<!UNSUPPORTED!>context(_: String)<!>
enum class E

<!UNSUPPORTED!>context(_: String)<!>
annotation class A

<!UNSUPPORTED!>context(_: String)<!>
object O

<!UNSUPPORTED!>context(_: String)<!>
typealias TA = Any

val objectExpression = <!UNRESOLVED_REFERENCE!>context<!>(<!UNRESOLVED_REFERENCE!>_<!><!SYNTAX!>: String<!>)<!SYNTAX!><!> object<!SYNTAX!><!> {}

<!UNSUPPORTED!>context(_: String)<!>
operator fun Any.getValue(thiz: Any?, metadata: Any?): Any = this

<!UNSUPPORTED!>context(_: String)<!>
operator fun Any.setValue(thiz: Any?, metadata: Any?, value: String): Any = this

<!UNSUPPORTED!>context(_: String)<!>
operator fun Any.provideDelegate(thiz: Any?, metadata: Any?): Any = this

var x: String = ""
    <!UNSUPPORTED!>context(_: String)<!> get
    <!UNSUPPORTED!>context(_: String)<!> set

var y: String = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>""<!>
    <!UNSUPPORTED!>context(_: String)<!> get() = ""
    <!UNSUPPORTED!>context(_: String)<!> set(v) {}

<!UNSUPPORTED!>context(_: String)<!>
var Any.d <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> ""

context(_: String)
var b = <!CONTEXT_PARAMETERS_WITH_BACKING_FIELD!>""<!>

context(<!SYNTAX!><!>)
fun emptyContext() {}

context(<!SYNTAX!><!>)
class EmptyContextClass

context(<!CONTEXT_PARAMETER_WITHOUT_NAME!>String<!>)
fun contextReceiverSyntax() {}

context(<!CONTEXT_PARAMETER_WITHOUT_NAME!>String<!>, _: Int)
fun mixedSyntax() {}

<!UNSUPPORTED!>context(<!CONTEXT_PARAMETER_WITHOUT_NAME, CONTEXT_PARAMETER_WITHOUT_NAME!>String<!>)<!>
class ClassWithContextReceiverSyntax {
    <!UNSUPPORTED!>context(<!CONTEXT_PARAMETER_WITHOUT_NAME!>String<!>)<!>
    constructor() {}
}

<!UNSUPPORTED!>context(<!CONTEXT_PARAMETER_WITH_DEFAULT!>x: String = ""<!>)<!>
class ClassWithContextDefaultValue

context(<!CONTEXT_PARAMETER_WITH_DEFAULT!>x: String = ""<!>)
val contextHasDefaultValue: String get() = ""

context(<!CONTEXT_PARAMETER_WITH_DEFAULT!>x: String = ""<!>)
fun contextHasDefaultValue() {}

context(<!WRONG_MODIFIER_TARGET!>vararg<!> x: String, <!VAL_OR_VAR_ON_FUN_PARAMETER!>var<!> y: String, <!VAL_OR_VAR_ON_FUN_PARAMETER!>val<!> z: String, <!WRONG_MODIFIER_TARGET!>crossinline<!> f1: () -> Unit, <!WRONG_MODIFIER_TARGET!>noinline<!> f2: () -> Unit)
<!NOTHING_TO_INLINE!>inline<!> fun contextHasModifier() {}
