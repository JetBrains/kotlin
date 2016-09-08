import java.nio.ByteBuffer
import java.nio.ByteOrder
import WireFormat.VARINT_INFO_BITS_COUNT
import WireFormat.VARINT_INFO_BITS_MASK
import WireFormat.VARINT_UTIL_BIT_MASK

/**
 * Created by user on 7/6/16.
 */

class CodedOutputStream(val output: java.io.OutputStream) {
    fun writeTag(fieldNumber: Int, type: WireType) {
        val tag = (fieldNumber shl 3) or type.ordinal
        writeInt32NoTag(tag)
    }

    fun writeInt32(fieldNumber: Int, value: Int?) {
        value ?: return
        writeTag(fieldNumber, WireType.VARINT)
        writeInt32NoTag(value)
    }

    // Note that unsigned integer types are stored as their signed counterparts with top bit
    // simply stored in the sign bit - similar to Java's protobuf implementation. Hence, all
    // methods, writing unsigned ints simply redirect call to corresponding signed-writing method
    fun writeUInt32(fieldNumber: Int, value: Int?) {
        value ?: return
        writeInt32(fieldNumber, value)
    }

    fun writeInt64(fieldNumber: Int, value: Long?) {
        value ?: return
        writeTag(fieldNumber, WireType.VARINT)
        writeInt64NoTag(value)
    }

    // See notes on unsigned integers implementation above
    fun writeUInt64(fieldNumber: Int, value: Long?) {
        value ?: return
        writeInt64(fieldNumber, value)
    }

    fun writeBool(fieldNumber: Int, value: Boolean?) {
        value ?: return
        writeTag(fieldNumber, WireType.VARINT)
        writeBoolNoTag(value)
    }

    fun writeBoolNoTag(value: Boolean) {
        writeInt32NoTag(if (value) 1 else 0)
    }

    // Writing enums is like writing one int32 number. Caller is responsible for converting enum-object to ordinal
    fun writeEnum(fieldNumber: Int, value: Int?) {
        value ?: return
        writeTag(fieldNumber, WireType.VARINT)
        writeEnumNoTag(value)
    }

    fun writeEnumNoTag(value: Int) {
        writeInt32NoTag(value)
    }

    fun writeSInt32(fieldNumber: Int, value: Int?) {
        value ?: return
        writeTag(fieldNumber, WireType.VARINT)
        writeSInt32NoTag(value)
    }

    fun writeSInt32NoTag(value: Int) {
        writeInt32NoTag((value shl 1) xor (value shr 31))
    }

    fun writeSInt64(fieldNumber: Int, value: Long?) {
        value ?: return
        writeTag(fieldNumber, WireType.VARINT)
        writeSInt64NoTag(value)
    }

    fun writeSInt64NoTag(value: Long) {
        writeInt64NoTag((value shl 1) xor (value shr 63))
    }

    fun writeFixed32(fieldNumber: Int, value: Int?) {
        value ?: return
        writeTag(fieldNumber, WireType.FIX_32)
        writeFixed32NoTag(value)
    }

    fun writeFixed32NoTag(value: Int) {
        writeLittleEndian(value)
    }

    // See notes on unsigned integers implementation above
    fun writeSFixed32(fieldNumber: Int, value: Int?) {
        value ?: return
        writeTag(fieldNumber, WireType.FIX_32)
        writeSFixed32NoTag(value)
    }

    fun writeSFixed32NoTag(value: Int) {
        writeLittleEndian(value)
    }

    fun writeFixed64(fieldNumber: Int, value: Long?) {
        value ?: return
        writeTag(fieldNumber, WireType.FIX_64)
        writeFixed64NoTag(value)
    }

    fun writeFixed64NoTag(value: Long) {
        writeLittleEndian(value)
    }

    // See notes on unsigned integers implementation above
    fun writeSFixed64(fieldNumber: Int, value: Long?) {
        value ?: return
        writeTag(fieldNumber, WireType.FIX_64)
        writeSFixed64NoTag(value)
    }

    fun writeSFixed64NoTag(value: Long) {
        writeLittleEndian(value)
    }

    fun writeDouble(fieldNumber: Int, value: Double?) {
        value ?: return
        writeTag(fieldNumber, WireType.FIX_64)
        writeDoubleNoTag(value)
    }

    fun writeDoubleNoTag(value: Double) {
        writeLittleEndian(value)
    }

    fun writeFloat(fieldNumber: Int, value: Float?) {
        value ?: return
        writeTag(fieldNumber, WireType.FIX_32)
        writeFloatNoTag(value)
    }

    fun writeFloatNoTag(value: Float) {
        writeLittleEndian(value)
    }

    fun writeString(fieldNumber: Int, value: String?) {
        value ?: return
        writeTag(fieldNumber, WireType.LENGTH_DELIMITED)
        writeStringNoTag(value)
    }

    fun writeStringNoTag(value: String) {
        writeInt32NoTag(value.length)
        output.write(value.toByteArray(Charsets.UTF_8))
    }

    fun writeBytes(fieldNumber: Int, value: ByteArray?) {
        value ?: return
        writeTag(fieldNumber, WireType.LENGTH_DELIMITED)
        writeBytesNoTag(value)
    }

    fun writeBytesNoTag(value: ByteArray) {
        writeInt32NoTag(value.size)
        output.write(value)
    }

    /** ============ Utility methods ==================
     *  They are left non-private for cases when one wants to implement her/his own protocol format.
     *  Then she/he can re-use low-level methods for operating with raw values, that are not annotated with Protobuf tags.
     */

    fun writeLittleEndian(value: Int?) {
        value ?: return
        val bb = ByteBuffer.allocate(4)
        bb.order(ByteOrder.LITTLE_ENDIAN)
        bb.putInt(value)
        output.write(bb.array())
    }

    fun writeLittleEndian(value: Long?) {
        value ?: return
        val bb = ByteBuffer.allocate(8)
        bb.order(ByteOrder.LITTLE_ENDIAN)
        bb.putLong(value)
        output.write(bb.array())
    }

    fun writeLittleEndian(value: Double?) {
        value ?: return
        val bb = ByteBuffer.allocate(8)
        bb.order(ByteOrder.LITTLE_ENDIAN)
        bb.putDouble(value)
        output.write(bb.array())
    }

    fun writeLittleEndian(value: Float?) {
        value ?: return
        val bb = ByteBuffer.allocate(4)
        bb.order(ByteOrder.LITTLE_ENDIAN)
        bb.putFloat(value)
        output.write(bb.array())
    }

    fun writeInt32NoTag(value: Int?) {
        value ?: return
        var curValue: Int = value

        // we have at most 32 information bits. With overhead of 1 bit per 7 bits we need at most 5 bytes for encoding
        val res = ByteArray(5)

        var resSize = 0
        do {
            // encode current 7 bits
            var curByte = (curValue and WireFormat.VARINT_INFO_BITS_MASK)

            // discard encoded bits. Note that unsigned shift is needed for cases with negative numbers
            curValue = curValue ushr VARINT_INFO_BITS_COUNT

            // check if there will be next byte in encoding and set util bit if needed
            if (curValue != 0) {
                curByte = curByte or VARINT_UTIL_BIT_MASK
            }

            res[resSize] = curByte.toByte()
            resSize++
        } while(curValue != 0)
        output.write(res, 0, resSize)
    }

    fun writeInt64NoTag(value: Long?) {
        value ?: return
        var curValue: Long = value

        // we have at most 64 information bits. With overhead of 1 bit per 7 bits we need at most 10 bytes for encoding
        val res = ByteArray(10)

        var resSize = 0
        while(curValue != 0L) {
            // encode current 7 bits
            var curByte = (curValue and VARINT_INFO_BITS_MASK.toLong())

            // discard encoded bits. Note that unsigned shift is needed for cases with negative numbers
            curValue = curValue ushr VARINT_INFO_BITS_COUNT

            // check if there will be next byte and set util bit if needed
            if (curValue != 0L) {
                curByte = curByte or VARINT_UTIL_BIT_MASK.toLong()
            }

            res[resSize] = curByte.toByte()
            resSize++
        }
        output.write(res, 0, resSize)
    }


}
