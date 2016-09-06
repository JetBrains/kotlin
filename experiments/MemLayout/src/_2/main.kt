package _2

import bridge
import kotlin.reflect.KProperty
import java.io.Closeable
import NativeFunction


operator fun NativeFunction.invoke(vararg args: Any?): Long {
    val convertedArgs = LongArray(args.size)

    val allocatedArgs = mutableListOf<Ptr<*>>()
    args.forEachIndexed { i, arg ->
        val convertedArg = when (arg) {
            null -> 0L
            is Long -> arg
            is Ptr<*> -> arg.ptr
            is String -> {
                val cString = CString.fromString(arg)
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

open class StructLayout(val size: Int)

open class Ptr<T : StructLayout>(val ptr: Long)


fun <T : StructLayout> alloc(layout: T): HeapPtr<T> {
    return HeapPtr<T>(bridge.malloc(layout.size))
}

class HeapPtr<T : StructLayout>(ptr: Long) : Ptr<T>(ptr), Closeable {
    override fun close() {
        free()
    }

    fun free() {
        bridge.free(ptr)
    }

}

class ByteField(val offset: Int) {
    operator fun getValue(thisRef: Ptr<*>, property: KProperty<*>): Byte {
        return bridge.getByte(thisRef.ptr + offset)
    }

    operator fun setValue(thisRef: Ptr<*>, property: KProperty<*>, value: Byte) {
        bridge.putByte(thisRef.ptr + offset, value)
    }
}

class ShortField(val offset: Int) {
    operator fun getValue(thisRef: Ptr<*>, property: KProperty<*>): Short {
        return bridge.getShort(thisRef.ptr + offset)
    }

    operator fun setValue(thisRef: Ptr<*>, property: KProperty<*>, value: Short) {
        bridge.putShort(thisRef.ptr + offset, value)
    }
}


class IntField(val offset: Int) {
    operator fun getValue(thisRef: Ptr<*>, property: KProperty<*>): Int {
        return bridge.getInt(thisRef.ptr + offset)
    }

    operator fun setValue(thisRef: Ptr<*>, property: KProperty<*>, value: Int) {
        bridge.putInt(thisRef.ptr + offset, value)
    }
}


class LongField(val offset: Int) {
    operator fun getValue(thisRef: Ptr<*>, property: KProperty<*>): Long {
        return bridge.getLong(thisRef.ptr + offset)
    }

    operator fun setValue(thisRef: Ptr<*>, property: KProperty<*>, value: Long) {
        bridge.putLong(thisRef.ptr + offset, value)
    }
}

class PtrField<T : StructLayout>(val offset: Int) {
    operator fun getValue(thisRef: Ptr<*>, property: KProperty<*>): Ptr<T> {
        return Ptr<T>(bridge.getLong(thisRef.ptr + offset))
    }

    operator fun setValue(thisRef: Ptr<*>, property: KProperty<*>, value: Ptr<T>) {
        bridge.putLong(thisRef.ptr + offset, value.ptr)
    }
}

class StructField<T : StructLayout>(val offset: Int) {
    operator fun getValue(thisRef: Ptr<*>, property: KProperty<*>) = Ptr<T>(thisRef.ptr + offset)
}

fun byteAt(offset: Int) = ByteField(offset)
fun shortAt(offset: Int) = ShortField(offset)
fun intAt(offset: Int) = IntField(offset)
fun longAt(offset: Int) = LongField(offset)
fun <T : StructLayout> ptrAt(offset: Int) = PtrField<T>(offset)
fun <T: StructLayout> structAt(offset: Int) = StructField<T>(offset)

class StructArray<T : StructLayout>(val elemLayout: T, length: Int) : StructLayout(elemLayout.size * length)

class PtrToStructArray<T : StructLayout>(ptr: Long, val elemLayout: StructLayout) : Ptr<StructArray<T>>(ptr) {
    operator fun get(index: Int) = Ptr<T>(ptr + elemLayout.size * index)
}

fun <T : StructLayout> allocArray(layout: StructArray<T>) = PtrToStructArray<T>(alloc(layout).ptr, layout.elemLayout)
fun <T : StructLayout> allocArray(elemLayout: T, length: Int) = allocArray(StructArray(elemLayout, length))

object IntBox : StructLayout(4)
var Ptr<IntBox>.value by intAt(0)

class PtrBox<T : StructLayout> : StructLayout(8)
var <T : StructLayout> Ptr<PtrBox<T>>.value by ptrAt<T>(0)

class CString(length: Int) : StructLayout(length + 1) {

    companion object {

        fun fromString(str: String): Ptr<CString> {
            val bytes = str.toByteArray(); // FIXME: encoding
            val result = alloc(CString(bytes.size))

            bytes.forEachIndexed { i, byte ->
                result[i] = byte
            }
            result[bytes.size] = 0.toByte()
            return result
        }
    }
}

operator fun Ptr<CString>.get(index: Int) = bridge.getByte(ptr + index)
operator fun Ptr<CString>.set(index: Int, value: Byte) = bridge.putByte(ptr + index, value)

fun Ptr<CString>.toKotlinString(): String {

    var length = 0
    while (this[length] != 0.toByte()) {
        ++length
    }

    val bytes = ByteArray(length)
    for (i in 0 .. length-1) {
        bytes.set(i, this[i])
    }

    return String(bytes) // FIXME: encoding
}

object timespec : StructLayout(16);

val Ptr<timespec>.tv_sec by longAt(0)
val Ptr<timespec>.tv_usec by longAt(8)

object stat64 : StructLayout(144);

val Ptr<stat64>.st_dev by intAt(0)
val Ptr<stat64>.st_mode by shortAt(4)
val Ptr<stat64>.st_nlink by shortAt(6)
val Ptr<stat64>.st_ino by longAt(8)
val Ptr<stat64>.st_atimespec by structAt<timespec>(32)
val Ptr<stat64>.st_mtimespec by structAt<timespec>(48)
val Ptr<stat64>.st_ctimespec by structAt<timespec>(64)
val Ptr<stat64>.st_birthtimespec by structAt<timespec>(80)
val Ptr<stat64>.st_size by longAt(96)

val stat64Fun = NativeFunction.lookup("stat\$INODE64")!!

object dirent64 : StructLayout(1048);
val Ptr<dirent64>.d_ino by longAt(0)
val Ptr<dirent64>.d_seekoff by longAt(8)
val Ptr<dirent64>.d_reclen by shortAt(16)
val Ptr<dirent64>.d_namelen by shortAt(18)
val Ptr<dirent64>.d_type by byteAt(20)
val Ptr<dirent64>.d_name by structAt<CString>(21)

val opendir = NativeFunction.lookup("opendir")!!
val readdir_r64 = NativeFunction.lookup("readdir_r\$INODE64")!!
val closedir = NativeFunction.lookup("closedir")!!

fun main(args: Array<String>) {

    val array = allocArray(IntBox, 5)
    array[0].value = 5
    array[1].value = 6

    println(array[0].value)
    println(array[1].value)

    alloc(stat64).use { s ->
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
        alloc(dirent64).use { entry ->
            alloc(PtrBox<dirent64>()).use { result ->

                while (true) {
                    val errno = readdir_r64(dirp, entry, result)
                    if (result.value.ptr == 0L) {
                        break
                    }
                    println(entry.d_name.toKotlinString())
                }

            }
        }
    } finally {
        closedir(dirp)
    }
};