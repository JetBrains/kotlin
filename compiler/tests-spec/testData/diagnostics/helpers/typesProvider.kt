fun getInt() = Any().hashCode()
fun getShort() = Any().hashCode().toShort()
fun getLong() = Any().hashCode().toLong()
fun getFloat() = Any().hashCode().toFloat()
fun getDouble() = Any().hashCode().toDouble()
fun getByte() = Any().hashCode().toByte()
fun getChar() = Any().hashCode().toChar()
fun getString() = Any().hashCode().toString()
fun getBoolean() = Any().hashCode() % 2 == 0
fun getNothing(): Nothing = throw Exception()
fun getUnit() = {}
fun getAny() = Any()
fun getList() = listOf<Int>()

class TypesProvider {
    fun getInt() = Any().hashCode()
    fun getShort() = Any().hashCode().toShort()
    fun getLong() = Any().hashCode().toLong()
    fun getFloat() = Any().hashCode().toFloat()
    fun getDouble() = Any().hashCode().toDouble()
    fun getByte() = Any().hashCode().toByte()
    fun getChar() = Any().hashCode().toChar()
    fun getString() = Any().hashCode().toString()
    fun getBoolean() = Any().hashCode() % 2 == 0
    fun getNothing(): Nothing = throw Exception()
    fun getUnit() = {}
    fun getAny() = Any()
    fun getList() = listOf<Int>()
}