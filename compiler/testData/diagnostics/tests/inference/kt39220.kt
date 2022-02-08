// !DIAGNOSTICS: -UNUSED_PARAMETER -FINAL_UPPER_BOUND

import kotlin.reflect.*

interface Foo {
    fun resolve(var1: Int): String
    fun resolve(var1: String): String

    suspend fun resolve2(var1: Int): String
    suspend fun resolve2(var1: String): String

    val Int.x1 get() = ""
    val String.x1 get() = ""

    var Int.x2
        get() = ""
        set(value) {}
    var String.x2
        get() = ""
        set(value) {}

    val Int.x3 get() = ""
    var String.x3
        get() = ""
        set(value) {}

    // CR on property with to receivers are forbidden
    fun <T: Foo> test() {
        // with LHS and property
        bar8<T>(Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>x1<!>)
        bar8<Foo>(Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>x1<!>)
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar8<!>(Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>x1<!>)

        // with LHS and mutable property
        bar8<T>(Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>x2<!>)
        bar8<Foo>(Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>x2<!>)
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar8<!>(Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>x2<!>)

        // with LHS and propery + mutable property (mixed)
        bar8<T>(Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>x3<!>)
        bar8<Foo>(Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>x3<!>)
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar8<!>(Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>x3<!>)
        bar9<T>(Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>x3<!>)
        bar9<Foo>(Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>x3<!>)
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar9<!>(Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>x3<!>)
    }
}

val Int.x1 get() = ""
val String.x1 get() = ""

fun <K> bar1(f: KFunction2<K, String, String>) {}

fun <K> bar2(f: KFunction2<<!CONFLICTING_PROJECTION!>out<!> K, String, String>) {}

fun <K> bar3(f: Any?) {}

fun <K> bar4(f: Function2<K, String, String>) {}

fun <K> bar5(f: suspend (K, String) -> String) {}

fun <K> bar6(f: KSuspendFunction2<K, String, String>) {}

fun <K> bar7(f: K.(String) -> String) {}

fun <K> bar8(f: KProperty2<K, Int, String>) {}

fun <K> bar9(f: KMutableProperty2<K, Int, String>) {}

fun <K> bar10(f: KProperty1<K, String>) {}

fun resolve(var2: Number, var1: Int) = ""
fun resolve(var2: Number, var1: String) = ""

fun <T : Foo, R: Number, D: Int> main() {
    // with LHS
    bar1<T>(Foo::resolve) // ERROR before the fix in NI
    bar1<Foo>(Foo::resolve) // OK
    bar1(Foo::resolve) // OK

    // without LHS
    bar1<R>(::resolve) // OK
    bar1<Number>(::resolve) // OK
    bar1(::resolve) // OK

    // with LHS and conflicting projection
    bar2<T>(Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>resolve<!>)
    bar2<Foo>(Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>resolve<!>)
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar2<!>(Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>resolve<!>)

    // with LHS and Any? expected type
    bar3<T>(Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>resolve<!>)
    bar3<Foo>(Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>resolve<!>)
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar3<!>(Foo::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>resolve<!>)

    // with LHS and `Function` expected type
    bar4<T>(Foo::resolve) // ERROR before the fix in NI
    bar4<Foo>(Foo::resolve) // OK
    bar4(Foo::resolve) // OK

    // with LHS and `SuspendFunction` expected type
    bar5<T>(Foo::resolve2) // ERROR before the fix in NI
    bar5<Foo>(Foo::resolve2) // OK
    bar5(Foo::resolve2) // OK

    // with LHS and `KSuspendFunction` expected type
    bar6<T>(Foo::resolve2) // ERROR before the fix in NI
    bar6<Foo>(Foo::resolve2) // OK
    bar6(Foo::resolve2) // OK

    // with LHS and sentension function expected type
    bar7<T>(Foo::resolve) // ERROR before the fix in NI
    bar7<Foo>(Foo::resolve) // OK
    bar7(Foo::resolve) // OK

    // with LHS and sentension function expected type
    bar10<D>(<!TYPE_MISMATCH!>Int::x1<!>) // ERROR before the fix in NI
    bar10<Int>(Int::x1) // OK
    bar10(Int::x1) // OK

    fun Int.ext() {
        // with LHS and sentension function expected type
        bar10<D>(<!TYPE_MISMATCH("KProperty1<TypeVariable(K), String>; KProperty0<String>")!>::x1<!>) // ERROR before the fix in NI
        bar10<Int>(<!TYPE_MISMATCH("KProperty1<TypeVariable(K), String>; KProperty0<String>")!>::x1<!>) // OK
        bar10(<!TYPE_MISMATCH("KProperty1<TypeVariable(K), String>; KProperty0<String>")!>::x1<!>) // OK
    }
}
