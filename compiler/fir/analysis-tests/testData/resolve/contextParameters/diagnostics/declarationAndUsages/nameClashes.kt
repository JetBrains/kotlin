// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

context(x: String, y: Int)
fun noClash(z: Boolean) {}

context(<!REDECLARATION!>x<!>: String, <!REDECLARATION!>x<!>: Int)
fun clashInContext(z: Boolean) {}

context(<!REDECLARATION!>x<!>: String, y: Int)
fun clashBetweenContextAndValueParameter(<!REDECLARATION!>x<!>: Boolean) {}

context(<!REDECLARATION!>x<!>: String, <!REDECLARATION!>x<!>: Int)
val clashInPropertyContext: String get() = ""

interface I {
    context(value: String)  // value is the name of the default setter parameter but it shouldn't cause a REDECLARATION
    abstract var Any.propertyWithContextNamedValue: String
}

context(<!REDECLARATION!>x<!>: String)
var clashBetweenPropertyContextAndSetterParam: String
    get() = ""
    set(<!REDECLARATION!>x<!>) {}

context(_: String, _: Int)
fun multipleUnnamed() {}

context(`_`: String, `_`: Int)
fun multipleUnnamedQuoted() {}

context(x: String)
inline fun <reified x> noClashWithTypeParam(b: x){}

context(x: String)
fun noClashInLocalFun() {
    fun local(x: String) {
        x.length
    }
}

context(a: String)
fun a() {
    a.length
}