// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-83250
// DIAGNOSTICS: -ERROR_SUPPRESSION
// FIR_DUMP

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> foo(x: In<T>, y: @kotlin.internal.NoInfer T, d: T, z: (T) -> Unit) {
    z(d)
}

interface In<in U>

interface Base
interface Derived : Base

fun overload(x: Base) {}

fun overload(x: Derived) {}

fun baz(x: In<Base>, base: Base, derived: Derived) {
    // T <: Base
    // Base <: T [NoInfer]
    // Derived <: T
    //
    // Should fix T to Derived and report and error
    <!TYPE_MISMATCH!>foo(x, base, derived) { x -> overload(x) }<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, in, interfaceDeclaration, lambdaLiteral, nullableType,
stringLiteral, typeParameter */
