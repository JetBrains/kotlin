// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring +EnableNameBasedDestructuringShortForm
// RENDER_DIAGNOSTICS_FULL_TEXT
data class Foo(val bar: String)
interface I
class Box<T>(val item: T)

fun test1(f: Foo) {
    val (<!UNRESOLVED_REFERENCE!>baz<!>) = f
}

fun <<!CONFLICTING_UPPER_BOUNDS!>T<!>> test2(f: T) where T : <!FINAL_UPPER_BOUND!>Foo<!>, T : I {
    val (<!UNRESOLVED_REFERENCE!>baz<!>) = f
}

fun test3(box: Box<out Foo>) {
    val (<!UNRESOLVED_REFERENCE!>baz<!>) = box.item
}

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, functionDeclaration, localProperty,
primaryConstructor, propertyDeclaration */
