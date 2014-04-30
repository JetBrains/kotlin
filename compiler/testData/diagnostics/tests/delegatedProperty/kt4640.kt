//KT-4640 "Trace is erased after resolution completion" exception

class ValueWrapper()
{
    var value: Int = 0

    fun get() = value
    fun set(v: Int) { value = v }
}

val foo by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>ValueWrapper()<!>
