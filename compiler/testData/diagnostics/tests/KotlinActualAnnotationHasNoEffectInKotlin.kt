// WITH_STDLIB

@KotlinActual
fun foo() {
    val a: KotlinActual = null!!
    val b: (KotlinActual) -> Unit = { x -> }
    val c: ((KotlinActual) -> Unit, (KotlinActual) -> Unit) -> ((KotlinActual) -> Unit) = { x, y -> { } }
    val d: (() -> KotlinActual, () -> KotlinActual) -> (() -> KotlinActual) = { x, y -> { null!! } }

    val e = KotlinActual::class
    val f = ::<!CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR!>KotlinActual<!>
    val g = KotlinActual()
}

typealias Duh = KotlinActual

@[KotlinActual]
fun bar() {}
