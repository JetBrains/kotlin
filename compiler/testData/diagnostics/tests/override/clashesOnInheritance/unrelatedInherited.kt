interface IA {
    fun method(): String
    val propVal: String
    var propVar: String
}

interface IB1 : IA
interface IB2 : IA

interface IGA<T> {
    fun method(): T
    val propVal: T
    var propVar: T
}

interface IGB1Str : IGA<String>
interface IGB2Str : IGA<String>
interface IGB3Int : IGA<Int>

interface IGB4T<T> : IGA<T>
interface IGB5T<T> : IGA<T>

interface IC : IB1, IB2

interface IGC1 : IGB1Str, IGB2Str

interface IGC2 : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>IGB1Str, IGB3Int<!>

interface IGC3<T> : IGB4T<T>, IGB5T<T>

interface IGC4<T> : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>IGB4T<T>, IGB5T<String><!>

interface IGC5 : IGB4T<String>, IGB5T<String>