package _1
import NativeFunction
import kotlin.reflect.KProperty

operator fun NativeFunction.invoke(vararg args: Any?): Long {
    val convertedArgs = LongArray(args.size)

    val allocatedArgs = mutableListOf<NativeStructPtr>()
    args.forEachIndexed { i, arg ->
        val convertedArg = when (arg) {
            null -> 0L
            is Long -> arg
            is NativeStructPtr -> arg.ptr
            is String -> {
                val cString = CStringPtr.fromString(arg)
                allocatedArgs.add(cString)
                cString.ptr
            }
            is Byte -> arg.toLong()
            is Char -> arg.toLong()
            is Short -> arg.toLong()
            is Int -> arg.toLong()
            else -> throw NotImplementedError()
        }
        convertedArgs[i] = convertedArg
    }
    try {
        return bridge.call(this.ptr, *convertedArgs)
    } finally {
        allocatedArgs.forEach { bridge.free(it.ptr) }
    }
}

open class NativeStructPtr(val ptr: Long) {
    companion object {
        class ByteField(val offset: Int) {
            operator fun getValue(thisRef: NativeStructPtr, property: KProperty<*>): Byte {
                return bridge.getByte(thisRef.ptr + offset)
            }

            operator fun setValue(thisRef: NativeStructPtr, property: KProperty<*>, value: Byte) {
                bridge.putByte(thisRef.ptr + offset, value)
            }
        }

        class ShortField(val offset: Int) {
            operator fun getValue(thisRef: NativeStructPtr, property: KProperty<*>): Short {
                return bridge.getShort(thisRef.ptr + offset)
            }

            operator fun setValue(thisRef: NativeStructPtr, property: KProperty<*>, value: Short) {
                bridge.putShort(thisRef.ptr + offset, value)
            }
        }

        class IntField(val offset: Int) {
            operator fun getValue(thisRef: NativeStructPtr, property: KProperty<*>): Int {
                return bridge.getInt(thisRef.ptr + offset)
            }

            operator fun setValue(thisRef: NativeStructPtr, property: KProperty<*>, value: Int) {
                bridge.putInt(thisRef.ptr + offset, value)
            }
        }

        class LongField(val offset: Int) {
            operator fun getValue(thisRef: NativeStructPtr, property: KProperty<*>): Long {
                return bridge.getLong(thisRef.ptr + offset)
            }

            operator fun setValue(thisRef: NativeStructPtr, property: KProperty<*>, value: Long) {
                bridge.putLong(thisRef.ptr + offset, value)
            }
        }

        class StructField<T : NativeStructPtr>(val cast: (Long) -> T, val offset: Int) {
            operator fun getValue(thisRef: NativeStructPtr, property: KProperty<*>): T {
                return cast(thisRef.ptr + offset);
            }
        }

        fun byteAt(offset: Int) = ByteField(offset)
        fun shortAt(offset: Int) = ShortField(offset)
        fun intAt(offset: Int) = IntField(offset)
        fun longAt(offset: Int) = LongField(offset)
        fun <T : NativeStructPtr> structAt(cast: (Long) -> T, offset: Int) = StructField(cast, offset)
    }
}

class Layout<T : NativeStructPtr>(val size: Int, val cast: (Long) -> T);

class PtrBox(ptr: Long) : NativeStructPtr(ptr) {
    companion object {
        val layout = Layout(8, ::PtrBox)
    }
}


class timespec(ptr: Long) : NativeStructPtr(ptr) {
    val tv_sec by longAt(0)
    val tv_usec by longAt(8)
}

class stat64(ptr: Long) : NativeStructPtr(ptr) {
    val st_dev by intAt(0)
    val st_mode by shortAt(4)
    val st_nlink by shortAt(6)
    val st_ino by longAt(8)
    val st_atimespec by structAt(::timespec, 32)
    val st_mtimespec by structAt(::timespec, 48)
    val st_ctimespec by structAt(::timespec, 64)
    val st_birthtimespec by structAt(::timespec, 80)
    val st_size by longAt(96)

    companion object {
        val layout = Layout(144, ::stat64)
    }

}
val stat64Fun = NativeFunction.lookup("stat\$INODE64")!!

class CStringPtr(ptr: Long) : NativeStructPtr(ptr) {

    companion object {
        fun fromString(str: String): CStringPtr {
            val bytes = str.toByteArray(); // FIXME: encoding
            val ptr = bridge.malloc(bytes.size + 1)
            bytes.forEachIndexed { i, byte ->
                bridge.putByte(ptr + i, byte)
            }
            bridge.putByte(ptr + bytes.size, 0)
            return CStringPtr(ptr)
        }
    }

    override fun toString(): String {
        var length = 0
        while (bridge.getByte(ptr + length) != 0.toByte()) {
            ++length
        }

        val bytes = ByteArray(length)
        for (i in 0 .. length-1) {
            bytes.set(i, bridge.getByte(ptr + i))
        }

        return String(bytes) // FIXME: encoding
    }

}

class dirent64(ptr: Long) : NativeStructPtr(ptr) {
    val d_ino by longAt(0)
    val d_seekoff by longAt(8)
    val d_reclen by shortAt(16)
    val d_namelen by shortAt(18)
    val d_type by byteAt(20)
    val d_name by structAt(::CStringPtr, 21)


    companion object {
        val layout = Layout(1048, ::dirent64)
    }
}

val opendir = NativeFunction.lookup("opendir")!!
val readdir_r64 = NativeFunction.lookup("readdir_r\$INODE64")!!
val closedir = NativeFunction.lookup("closedir")!!

fun <T : NativeStructPtr> malloc(layout: Layout<T>) = layout.cast(bridge.malloc(layout.size))
fun <T : NativeStructPtr, R> malloc(layout: Layout<T>, action: (T) -> R): R {
    val s = malloc(layout)
    try {
        return action(s)
    } finally {
        bridge.free(s.ptr)
    }
}

fun main(args: Array<String>) {

    malloc(stat64.layout) { s ->
        val res = stat64Fun("/etc/passwd", s)
        println(res)
        with(s) {
            println("st_dev=$st_dev")
            println("st_mode=$st_mode")
            println("st_nlink=$st_nlink")
            println("st_ino=$st_ino")
            println("st_mtimespec.tv_sec=${st_mtimespec.tv_sec}")
            println("st_size=$st_size")
        }
    }

    val dirp = opendir("/tmp")
    try {
        malloc(dirent64.layout) { entry ->
            malloc(PtrBox.layout) { result ->

                while (true) {
                    val errno = readdir_r64(dirp, entry, result)
                    if (bridge.getLong(result.ptr) == 0L) {
                        break
                    }
                    println(entry.d_name)
                }

            }
        }
    } finally {
        closedir(dirp)
    }


}