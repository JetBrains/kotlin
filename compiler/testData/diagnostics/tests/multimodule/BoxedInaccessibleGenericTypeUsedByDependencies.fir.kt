// MODULE: missing

class InaccessibleType<ITTP>

// MODULE: library(missing)

class Box<BTP>

fun produceBoxedInaccessibleType(): Box<InaccessibleType<Any?>> = Box()
fun consumeBoxedInaccessibleType(arg: Box<InaccessibleType<Any?>>) {}

// MODULE: main(library)

fun test() {
    consumeBoxedInaccessibleType(<!ARGUMENT_TYPE_MISMATCH("Box<InaccessibleType<kotlin.Any?>>; Box<CapturedType(out ERROR CLASS: Inconsistent type: InaccessibleType<kotlin/Any?> (parameters.size = 0, arguments.size = 1))>")!>produceBoxedInaccessibleType()<!>)
}
