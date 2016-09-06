import sun.misc.Unsafe

object bridge {

    init {
        System.loadLibrary("bridge")
    }

    fun lookup(name: String): Long {
        return findSym(name)
    }

    fun call(funcPtr: Long, vararg args: Long): Long {
        when (args.size) {
            0 -> return call0(funcPtr)
            1 -> return call1(funcPtr, args[0])
            2 -> return call2(funcPtr, args[0], args[1])
            3 -> return call3(funcPtr, args[0], args[1], args[2])
            4 -> return call4(funcPtr, args[0], args[1], args[2], args[3])
            else -> throw NotImplementedError()
        }
    }

    private external fun findSym(name: String): Long
    private external fun call0(ptr: Long): Long
    private external fun call1(ptr: Long, arg1: Long): Long
    private external fun call2(ptr: Long, arg1: Long, arg2: Long): Long
    private external fun call3(ptr: Long, arg1: Long, arg2: Long, arg3: Long): Long
    private external fun call4(ptr: Long, arg1: Long, arg2: Long, arg3: Long, arg4: Long): Long

    fun malloc(size: Int): Long = unsafe.allocateMemory(size.toLong())
    fun free(ptr: Long) = unsafe.freeMemory(ptr)

    fun getLong(ptr: Long): Long = unsafe.getLong(ptr)
    fun putLong(ptr: Long, value: Long) = unsafe.putLong(ptr, value)

    fun getInt(ptr: Long): Int = unsafe.getInt(ptr)
    fun putInt(ptr: Long, value: Int) = unsafe.putInt(ptr, value)

    fun getShort(ptr: Long): Short = unsafe.getShort(ptr)
    fun putShort(ptr: Long, value: Short) = unsafe.putShort(ptr, value)

    fun getByte(ptr: Long): Byte = unsafe.getByte(ptr)
    fun putByte(ptr: Long, value: Byte) = unsafe.putByte(ptr, value)

    private val unsafe = with(Unsafe::class.java.getDeclaredField("theUnsafe")) {
        isAccessible = true
        return@with this.get(null) as Unsafe
    }
}

class NativeFunction(val ptr: Long) {

    companion object {
        fun lookup(name: String): NativeFunction? {
            val funcPtr = bridge.lookup(name)
            return if (funcPtr == 0L) {
                null
            } else {
                NativeFunction(funcPtr)
            }
        }
    }
}