package kotlin_native.interop

// TODO: what about equals/hashCode?

open class NativeRef(val ptr: NativePtr) {
    open class Type<T : NativeRef>(val byPtr: (NativePtr) -> T)
    open class TypeWithSize<T : NativeRef>(val size: Int, byPtr: (NativePtr) -> T) : NativeRef.Type<T>(byPtr)
}

fun <T : NativeRef> NativePtr?.asRef(type: NativeRef.Type<T>) = this?.let { type.byPtr(this) }
fun NativeRef?.getNativePtr() = this?.ptr

class Int8Box(ptr: NativePtr) : NativeRef(ptr) {
    companion object : NativeRef.TypeWithSize<Int8Box>(1, ::Int8Box)
    var value: Byte
        get()       = bridge.getInt8(ptr)
        set(value)  = bridge.putInt8(ptr, value)
}

class Int16Box(ptr: NativePtr) : NativeRef(ptr) {
    companion object : NativeRef.TypeWithSize<Int16Box>(2, ::Int16Box)
    var value: Short
        get()       = bridge.getInt16(ptr)
        set(value)  = bridge.putInt16(ptr, value)
}

class Int32Box(ptr: NativePtr) : NativeRef(ptr) {
    companion object : NativeRef.TypeWithSize<Int32Box>(4, ::Int32Box)
    var value: Int
        get()       = bridge.getInt32(ptr)
        set(value)  = bridge.putInt32(ptr, value)
}

class NativePtrBox(ptr: NativePtr) : NativeRef(ptr) {
    companion object : NativeRef.TypeWithSize<NativePtrBox>(8, ::NativePtrBox) // TODO: 64-bit specific
    var value: NativePtr?
        get()       = bridge.getPtr(ptr)
        set(value)  = bridge.putPtr(ptr, value)
}

class Int64Box(ptr: NativePtr) : NativeRef(ptr) {
    companion object : NativeRef.TypeWithSize<Int64Box>(8, ::Int64Box)
    var value: Long
        get()       = bridge.getInt64(ptr)
        set(value)  = bridge.putInt64(ptr, value)
}

class RefBox<T : NativeRef>(ptr: NativePtr, val referentType: NativeRef.Type<T>) : NativeRef(ptr) {
    companion object {
        infix fun <T : NativeRef> of(type: NativeRef.Type<T>) = NativeRef.TypeWithSize<RefBox<T>>(8, { RefBox(it, type) })  // TODO: 64-bit specific
    }

    var value: T?
        get()       = bridge.getPtr(ptr).asRef(referentType)
        set(value)  = bridge.putPtr(ptr, value.getNativePtr())
}

val <T : NativeRef> NativeRef.Type<T>.ref: NativeRef.TypeWithSize<RefBox<T>>
    get() = Ref to this

object Ref {
    infix fun <T : NativeRef> to(type: NativeRef.Type<T>) = RefBox.Companion of type
    infix fun to(type: Byte.Companion) = Int8Box.Companion
    infix fun to(type: Short.Companion) = Int16Box.Companion
    infix fun to(type: Int.Companion) = Int32Box.Companion
    infix fun to(type: Long.Companion) = Int64Box.Companion
}

open class NativeStruct(ptr: NativePtr) : NativeRef(ptr) {
    open class Type<T : NativeStruct>(size: Int, byPtr: (NativePtr) -> T) : NativeRef.TypeWithSize<T>(size, byPtr)

    companion object {
        class FieldAt<T : NativeRef>(val type: NativeRef.Type<T>, val offset: Int) {
            operator fun getValue(thisRef: NativeStruct, property: kotlin.reflect.KProperty<*>): T {
                return type.byPtr(thisRef.ptr.displacedBy(offset))
            }
        }

        infix fun <T : NativeRef> NativeRef.Type<T>.at(offset: Int) = NativeStruct.Companion.FieldAt(this, offset)
    }
}

class NativeArray<T : NativeRef>(ptr: NativePtr, val elemType: NativeRef.TypeWithSize<T>) : NativeRef(ptr) {
    operator fun get(index: Int): T {
        return elemType.byPtr(ptr.displacedBy(index * elemType.size))
    }

    companion object {

        class Type<T : NativeRef> (val elemType: NativeRef.TypeWithSize<T>) :
                NativeRef.Type<NativeArray<T>>({ ptr -> NativeArray(ptr, elemType) }) {

            infix fun length(length: Int) =
                    NativeRef.TypeWithSize(elemType.size * length, { ptr -> NativeArray(ptr, elemType) })
        }

        infix fun <T : NativeRef> of(elemType: NativeRef.TypeWithSize<T>) = NativeArray.Companion.Type(elemType)

        fun <T : NativeRef> byRefToFirstElem(ref: T, refType: NativeRef.TypeWithSize<T>) = NativeArray(ref.ptr, refType)
    }
}

operator fun <T : NativeRef> NativeRef.TypeWithSize<T>.get(length: Int) = NativeArray.Companion of this length length
object array {

    // array(type)
    operator fun <T : NativeRef> invoke(type: NativeRef.TypeWithSize<T>) = NativeArray.Companion of type

    // array[length](type)
    operator fun get(length: Int) = array.ArrayWithLength(length)

    class ArrayWithLength(val length: Int) {
        infix fun <T : NativeRef> of(type: NativeRef.TypeWithSize<T>) = NativeArray.Companion of type length length

        operator fun <T : NativeRef> invoke(type: NativeRef.TypeWithSize<T>) = this of type
    }

}


class CString private constructor(internal val array: NativeArray<Int8Box>) : NativeRef(array.ptr) {
    companion object {
        fun fromArray(array: NativeArray<Int8Box>) = CString(array)
    }

    fun length(): Int {
        var res = 0
        while (array[res].value != 0.toByte()) {
            ++res
        }
        return res
    }

    override fun toString(): String {
        val bytes = ByteArray(this.length())
        bytes.forEachIndexed { i, byte ->
            bytes[i] = this.array[i].value
        }
        return String(bytes) // TODO: encoding
    }
}