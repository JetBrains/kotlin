// LATEST_LV_DIFFERENCE
// RENDER_DIAGNOSTICS_FULL_TEXT
// MODULE: missing

class InaccessibleType<ITTP>

// MODULE: library(missing)

class Box<BTP>

fun produceBoxedInaccessibleType(): Box<InaccessibleType<Any?>> = Box()
fun consumeBoxedInaccessibleType(arg: Box<InaccessibleType<Any?>>) {}

// MODULE: main(library)

fun test() {
    <!MISSING_DEPENDENCY_CLASS!>consumeBoxedInaccessibleType<!>(<!MISSING_DEPENDENCY_CLASS!>produceBoxedInaccessibleType<!>())
}

fun test2() {
    val a = <!MISSING_DEPENDENCY_CLASS!>produceBoxedInaccessibleType<!>()
    <!MISSING_DEPENDENCY_CLASS!>consumeBoxedInaccessibleType<!>(<!ARGUMENT_TYPE_MISMATCH, MISSING_DEPENDENCY_CLASS!>a<!>)
}
