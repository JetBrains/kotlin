package ppp

class X<T1> {
    fun <T2> f(xxxValue1: T1, xxxValue2: T2, xxxValue3: (T2) -> Unit){}

    fun foo(xxx<caret>)
}

// EXIST: { lookupString: "xxxValue1", itemText: "xxxValue1: T1", tailText: null }
// NOTHING_ELSE
