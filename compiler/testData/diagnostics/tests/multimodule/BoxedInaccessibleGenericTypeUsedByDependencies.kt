// RENDER_DIAGNOSTICS_FULL_TEXT
// MODULE: missing

class InaccessibleType<ITTP>

// MODULE: library(missing)

class Box<BTP>

fun produceBoxedInaccessibleType(): Box<InaccessibleType<Any?>> = Box()
fun consumeBoxedInaccessibleType(arg: Box<InaccessibleType<Any?>>) {}

// MODULE: main(library)

fun test() {
    consumeBoxedInaccessibleType(produceBoxedInaccessibleType())
}

fun test2() {
    val a = produceBoxedInaccessibleType()
    consumeBoxedInaccessibleType(a)
}
