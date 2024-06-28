// ISSUE: KT-54920

sealed interface I {
    class C : I
}

sealed interface IAbstract {
    abstract class C : IAbstract {
        class S1 : C()
        class S2 : C()
    }
}

sealed interface ISealed {
    sealed class C : ISealed {
        class S1 : C()
        class S2 : C()
    }
}

fun testDoubleWhen(x: I): Int {
    val a = when (x) { is I.C -> 1 }
    val b = when (x) { <!USELESS_IS_CHECK!>is I.C<!> -> 2 }
    return a + b
}

fun testDoubleWhen(x: IAbstract): Int {
    val a = when (x) { is IAbstract.C -> 1 }
    val b = when (x) { <!USELESS_IS_CHECK!>is IAbstract.C<!> -> 2 }
    return a + b
}

fun testDoubleWhen(x: ISealed): Int {
    val a = when (x) { is ISealed.C -> 1 }
    val b = when (x) { <!USELESS_IS_CHECK!>is ISealed.C<!> -> 2 }
    return a + b
}
