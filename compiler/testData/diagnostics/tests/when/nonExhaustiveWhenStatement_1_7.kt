// FIR_IDENTICAL
// LANGUAGE: +WarnAboutNonExhaustiveWhenOnAlgebraicTypes +ProhibitNonExhaustiveWhenOnAlgebraicTypes

enum class SomeEnum {
    A, B
}

sealed class Base {
    class A : Base()
    class B : Base()
}

sealed interface IBase {
    interface A : IBase
    interface B : IBase
}

// ------------------ not null ------------------

fun test_1(x: SomeEnum) {
    <!NO_ELSE_IN_WHEN!>when<!> (x) {
        SomeEnum.A -> ""
    }
}

fun test_2(x: Base) {
    <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is Base.A -> ""
    }
}

fun test_3(x: IBase) {
    <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is IBase.A -> ""
    }
}

fun test_4(x: Boolean) {
    <!NO_ELSE_IN_WHEN!>when<!> (x) {
        true -> ""
    }
}

// ------------------ nullable ------------------

fun test_5(x: SomeEnum?) {
    <!NO_ELSE_IN_WHEN!>when<!> (x) {
        SomeEnum.A -> ""
        SomeEnum.B -> ""
    }

    <!NO_ELSE_IN_WHEN!>when<!> (x) {
        SomeEnum.A -> ""
        null -> ""
    }
}

fun test_6(x: Base?) {
    <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is Base.A -> ""
        is Base.B -> ""
    }

    <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is Base.A -> ""
        null -> ""
    }
}

fun test_7(x: IBase?) {
    <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is IBase.A -> ""
        is IBase.B -> ""
    }
    <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is IBase.A -> ""
        null -> ""
    }
}

fun test_8(x: Boolean?) {
    <!NO_ELSE_IN_WHEN!>when<!> (x) {
        true -> ""
        false -> ""
    }

    <!NO_ELSE_IN_WHEN!>when<!> (x) {
        true -> ""
        null -> ""
    }
}

// ------------------ with else ------------------

fun test_9(x: SomeEnum?) {
    when (x) {
        SomeEnum.A -> ""
        else -> ""
    }
}

fun test_10(x: Base?) {
    when (x) {
        is Base.A -> ""
        else -> ""
    }
}

fun test_11(x: IBase?) {
    when (x) {
        is IBase.A -> ""
        else -> ""
    }
}

fun test_12(x: Boolean?) {
    when (x) {
        true -> ""
        else -> ""
    }
}

// ------------------ exhaustive ------------------

fun test_13(x: SomeEnum?) {
    when (x) {
        SomeEnum.A -> ""
        SomeEnum.B -> ""
        null -> ""
    }
}

fun test_14(x: Base?) {
    when (x) {
        is Base.A -> ""
        is Base.B -> ""
        null -> ""
    }
}

fun test_15(x: IBase?) {
    when (x) {
        is IBase.A -> ""
        is IBase.B -> ""
        null -> ""
    }
}

fun test_16(x: Boolean?) {
    when (x) {
        true -> ""
        false -> ""
        null -> ""
    }
}
