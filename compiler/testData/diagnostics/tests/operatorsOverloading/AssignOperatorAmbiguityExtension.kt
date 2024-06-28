// FIR_IDENTICAL
// ISSUE: KT-62138
class HashMap<K, V>(
    private val defaultValue: V
) {
    operator fun get(key: K): V = defaultValue
    operator fun set(key: K, value: V) { }
}

private class X

private operator fun X?.plus(p: Int) = X()
private operator fun X?.plusAssign(p: Int) { }

class C {
    private val map = HashMap<String, X>(defaultValue = X())

    fun f(): Any? {
        map[""] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> 1
        return map[""]
    }
}