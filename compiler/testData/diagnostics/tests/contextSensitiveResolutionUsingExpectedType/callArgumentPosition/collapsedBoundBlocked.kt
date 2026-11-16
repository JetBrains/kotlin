// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89822

interface Marker

val Marker.bar: String get() = ""

interface BlockedBase {
    companion object : Marker
}

interface ResolvedDerived : BlockedBase {
    companion object {
        val bar: Impl get() = Impl()
    }
}

interface ResolvedBase {
    companion object {
        val bar: Impl get() = Impl()
    }
}

interface BlockedDerived : ResolvedBase {
    companion object : Marker
}

class Impl : ResolvedDerived, BlockedDerived

fun <T> blockedBaseResolvedDerived(t: T) where T : BlockedBase, T : ResolvedDerived {}
fun <T> resolvedBaseBlockedDerived(t: T) where T : ResolvedBase, T : BlockedDerived {}

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>blockedBaseResolvedDerived<!>(<!UNRESOLVED_REFERENCE!>bar<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>resolvedBaseBlockedDerived<!>(<!UNRESOLVED_REFERENCE!>bar<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, getter, interfaceDeclaration,
objectDeclaration, propertyDeclaration, propertyWithExtensionReceiver, typeConstraint, typeParameter */
