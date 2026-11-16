// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89822

interface Marker

val Marker.bar: String get() = ""

sealed class Parent {
    companion object {
        val bar: MyClass get() = MyClass()
    }

    class MyClass : Parent(), Clean, BlockedByExtension, BlockedByContext
}

interface Clean {
    companion object
}

interface BlockedByExtension {
    companion object : Marker
}

interface BlockedByContext {
    companion object {
        context(s: String)
        val bar: String get() = ""
    }
}

fun <T> upperClean(t: T) where T : Parent, T : Clean {}
fun <T> upperBlockedByExtension(t: T) where T : Parent, T : BlockedByExtension {}
fun <T> upperBlockedByContext(t: T) where T : Parent, T : BlockedByContext {}

fun <T : Clean> lowerAndUpperClean(a: T, b: T) {}
fun <T : BlockedByExtension> lowerAndUpperBlockedByExtension(a: T, b: T) {}
fun <T : BlockedByContext> lowerAndUpperBlockedByContext(a: T, b: T) {}

fun <T> returnsGeneric(t: T): T = t
fun <T : Clean> returnsGenericClean(t: T): T = t
fun <T : BlockedByExtension> returnsGenericBlockedByExtension(t: T): T = t
fun <T : BlockedByContext> returnsGenericBlockedByContext(t: T): T = t

context(s: String)
fun test() {
    upperClean(bar)
    upperBlockedByExtension(bar)
    upperBlockedByContext(bar)

    lowerAndUpperClean(Parent.MyClass(), bar)
    lowerAndUpperBlockedByExtension(Parent.MyClass(), bar)
    lowerAndUpperBlockedByContext(Parent.MyClass(), bar)

    <!CANNOT_INFER_PARAMETER_TYPE!>returnsGeneric<!>(<!UNRESOLVED_REFERENCE!>bar<!>)
    val a: Parent.MyClass = returnsGeneric(bar)
    val b: Parent.MyClass = returnsGenericClean(bar)
    val c: Parent.MyClass = returnsGenericBlockedByExtension(bar)
    val d: Parent.MyClass = returnsGenericBlockedByContext(bar)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, functionDeclarationWithContext, getter,
interfaceDeclaration, nestedClass, objectDeclaration, propertyDeclaration, propertyDeclarationWithContext,
propertyWithExtensionReceiver, sealed, typeConstraint, typeParameter */
