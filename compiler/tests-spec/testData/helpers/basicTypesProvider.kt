tests._basicTypesProvider

fun getInt(arg: Any) = arg.hashCode()
fun getShort(arg: Any) = arg.hashCode().toShort()
fun getLong(arg: Any) = arg.hashCode().toLong()
fun getFloat(arg: Any) = arg.hashCode().toFloat()
fun getDouble(arg: Any) = arg.hashCode().toDouble()
fun getByte(arg: Any) = arg.hashCode().toByte()
fun getChar(arg: Any) = arg.hashCode().toChar()
fun getString(arg: Any) = arg.hashCode().toString()
fun getBoolean(arg: Any) = arg.hashCode() % 2 == 0
fun getNothing() = throw Exception()
fun getUnit() = {}
fun getAny() = Any()
fun getList() = mutableListOf<Int>()

class _BasicTypesProvider {
    fun getInt(arg: Any) = arg.hashCode()
    fun getShort(arg: Any) = arg.hashCode().toShort()
    fun getLong(arg: Any) = arg.hashCode().toLong()
    fun getFloat(arg: Any) = arg.hashCode().toFloat()
    fun getDouble(arg: Any) = arg.hashCode().toDouble()
    fun getByte(arg: Any) = arg.hashCode().toByte()
    fun getChar(arg: Any) = arg.hashCode().toChar()
    fun getString(arg: Any) = arg.hashCode().toString()
    fun getBoolean(arg: Any) = arg.hashCode() % 2 == 0
    fun getNothing() = throw Exception()
    fun getUnit() = {}
    fun getAny() = Any()
    fun getList() = mutableListOf<Int>()
}