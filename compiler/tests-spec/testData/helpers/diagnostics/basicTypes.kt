fun getInt(arg: Any = Any()) = arg.hashCode()
fun getShort(arg: Any = Any()) = arg.hashCode().toShort()
fun getLong(arg: Any = Any()) = arg.hashCode().toLong()
fun getFloat(arg: Any = Any()) = arg.hashCode().toFloat()
fun getDouble(arg: Any = Any()) = arg.hashCode().toDouble()
fun getByte(arg: Any = Any()) = arg.hashCode().toByte()
fun getChar(arg: Any = Any()) = arg.hashCode().toChar()
fun getString(arg: Any = Any()) = arg.hashCode().toString()
fun getBoolean(arg: Any = Any()) = arg.hashCode() % 2 == 0
fun getNothing(): Nothing = throw Exception()
fun getUnit() = {}
fun getAny() = Any()
fun getList() = mutableListOf<Int>()

class _BasicTypesProvider {
    fun getInt(arg: Any = Any()) = arg.hashCode()
    fun getShort(arg: Any = Any()) = arg.hashCode().toShort()
    fun getLong(arg: Any = Any()) = arg.hashCode().toLong()
    fun getFloat(arg: Any = Any()) = arg.hashCode().toFloat()
    fun getDouble(arg: Any = Any()) = arg.hashCode().toDouble()
    fun getByte(arg: Any = Any()) = arg.hashCode().toByte()
    fun getChar(arg: Any = Any()) = arg.hashCode().toChar()
    fun getString(arg: Any = Any()) = arg.hashCode().toString()
    fun getBoolean(arg: Any = Any()) = arg.hashCode() % 2 == 0
    fun getNothing(): Nothing = throw Exception()
    fun getUnit() = {}
    fun getAny() = Any()
    fun getList() = mutableListOf<Int>()
}