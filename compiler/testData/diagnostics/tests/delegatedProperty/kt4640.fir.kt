//KT-4640 "Trace is erased after resolution completion" exception

class ValueWrapper()
{
    var backingValue: Int = 0

    fun getValue() = backingValue
    fun setValue(v: Int) { backingValue = v }
}

val foo by <!INAPPLICABLE_CANDIDATE!>ValueWrapper()<!>
