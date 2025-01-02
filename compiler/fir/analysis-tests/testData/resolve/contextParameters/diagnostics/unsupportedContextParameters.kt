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

    context(_: String)
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
    context(_: String) <!SYNTAX!>get<!>
    context(_: String) <!SYNTAX!>set<!>

var y: String = ""
    context(_: String) <!SYNTAX!>get<!><!SYNTAX!>(<!><!SYNTAX!>)<!> <!SYNTAX!>=<!> <!SYNTAX!>"<!><!SYNTAX!>"<!>
    context(_: String) <!SYNTAX!>set<!><!SYNTAX!>(<!><!SYNTAX!>v<!><!SYNTAX!>)<!> <!FUNCTION_DECLARATION_WITH_NO_NAME!><!SYNTAX!><!>{}<!>

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

<!UNSUPPORTED!>context(String)<!>
class ClassWithContextReceiverSyntax {
    <!UNSUPPORTED!>context(String)<!>
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
