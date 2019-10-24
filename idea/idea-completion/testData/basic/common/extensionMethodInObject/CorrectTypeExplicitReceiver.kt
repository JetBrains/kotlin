interface T
interface B: T
interface C: T

object A {
    fun Any.fooForAny() {}

    fun T.fooForT() {}
    fun B.fooForB() {}
    fun C.fooForC() {}

    fun <TT> TT.fooForAnyGeneric() {}
    fun <TT: T> TT.fooForTGeneric() {}
    fun <TT: B> TT.fooForBGeneric() {}
    fun <TT: C> TT.fooForCGeneric() {}

    fun fooNoReceiver() {}
}

fun usage(b: B) {
    b.foo<caret>
}

// INVOCATION_COUNT: 2
// EXIST: { lookupString: "fooForAny", itemText: "fooForAny" }

// EXIST: { lookupString: "fooForT", itemText: "fooForT" }
// EXIST: { lookupString: "fooForB", itemText: "fooForB" }

// EXIST: { lookupString: "fooForTGeneric", itemText: "fooForTGeneric" }
// EXIST: { lookupString: "fooForBGeneric", itemText: "fooForBGeneric" }

// ABSENT: fooForC
// ABSENT: fooForCGeneric
// ABSENT: fooNoReceiver