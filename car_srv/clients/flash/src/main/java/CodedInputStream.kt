import java.nio.ByteBuffer
import java.nio.ByteOrder
import WireFormat.VARINT_INFO_BITS_COUNT
import WireFormat.VARINT_INFO_BITS_MASK
import WireFormat.VARINT_UTIL_BIT_MASK

/**
 * Created by Dmitry Savvinov on 7/6/16.
 *
 * Hides details of work with Protobuf encoding
 *
 * Note that CodedInputStream reads protobuf-defined types from stream (such as int32, sint32, etc),
 * while CodedOutputStream has methods for writing Kotlin-types (such as Boolean, Int, Long, Short, etc)
 *
 */

// TODO: refactor correctness checks into readTag
class CodedInputStream(input: java.io.InputStream) {
    val bufferedInput: java.io.BufferedInputStream
    init {
        bufferedInput = java.io.BufferedInputStream(input)  // TODO: Java's realization uses hand-written buffers. Why?
    }

    fun readInt32(expectedFieldNumber: Int): Int {
        val tag = readTag(expectedFieldNumber, WireType.VARINT)
        val actualFieldNumber = WireFormat.getTagFieldNumber(tag)
        val actualWireType = WireFormat.getTagWireType(tag)
        checkFieldCorrectness(expectedFieldNumber, actualFieldNumber, WireType.VARINT, actualWireType)
        return readInt32NoTag()
    }

    // Note that unsigned integer types are stored as their signed counterparts with top bit
    // simply stored in the sign bit - similar to Java's protobuf implementation. Hence, all
    // methods reading unsigned ints simply redirect call to corresponding signed-reading method
    fun readUInt32(expectedFieldNumber: Int): Int {
        val tag = readTag(expectedFieldNumber, WireType.VARINT)
        return readUInt32NoTag()
    }

    fun readUInt32NoTag(): Int {
        return readInt32NoTag()
    }

    fun readInt64(expectedFieldNumber: Int): Long {
        val tag = readTag(expectedFieldNumber, WireType.VARINT)
        return readInt64NoTag()
    }

    // See note on unsigned integers implementations above
    fun readUInt64(expectedFieldNumber: Int): Long {
        val tag = readTag(expectedFieldNumber, WireType.VARINT)
        return readUInt64NoTag()
    }

    fun readUInt64NoTag(): Long {
        return readInt64NoTag()
    }

    fun readBool(expectedFieldNumber: Int): Boolean {
        val tag = readTag(expectedFieldNumber, WireType.VARINT)
        return readBoolNoTag()
    }

    fun readBoolNoTag(): Boolean {
        val readValue = readInt32NoTag()
        val boolValue = when (readValue) {
            0 -> false
            1 -> true
            else -> throw InvalidProtocolBufferException("Expected boolean-encoding (1 or 0), got $readValue")
        }
        return boolValue
    }

    // Reading enums is like reading one int32 number. Caller is responsible for converting this ordinal to enum-object
    fun readEnum(expectedFieldNumber: Int): Int {
        val tag = readTag(expectedFieldNumber, WireType.VARINT)
        return readEnumNoTag()
    }

    fun readEnumNoTag(): Int {
        return readUInt32NoTag()
    }

    fun readSInt32(expectedFieldNumber: Int): Int {
        val tag = readTag(expectedFieldNumber, WireType.VARINT)
        return readSInt32NoTag()
    }

    fun readSInt32NoTag(): Int {
        return readZigZag32NoTag()
    }

    fun readSInt64(expectedFieldNumber: Int): Long {
        val tag = readTag(expectedFieldNumber, WireType.VARINT)
        return readZigZag64NoTag()
    }

    fun readFixed32(expectedFieldNumber: Int): Int {
        val tag = readTag(expectedFieldNumber, WireType.FIX_32)
        return readFixed32NoInt()
    }

    fun readFixed32NoInt(): Int {
        return readLittleEndianInt()
    }

    fun readSFixed32(expectedFieldNumber: Int): Int {
        val tag = readTag(expectedFieldNumber, WireType.FIX_32)
        return readSFixed32NoTag()
    }

    fun readSFixed32NoTag(): Int {
        return readLittleEndianInt()
    }

    fun readFixed64(expectedFieldNumber: Int): Long {
        val tag = readTag(expectedFieldNumber, WireType.FIX_64)
        return readFixed64NoTag()
    }

    fun readFixed64NoTag(): Long {
        return readLittleEndianLong()
    }

    fun readSFixed64(expectedFieldNumber: Int): Long {
        val tag = readTag(expectedFieldNumber, WireType.FIX_64)
        return readSFixed64NoTag()
    }

    fun readSFixed64NoTag(): Long {
        return readLittleEndianLong()
    }

    fun readDouble(expectedFieldNumber: Int): Double {
        val tag = readTag(expectedFieldNumber, WireType.FIX_64)
        return readDoubleNoTag()
    }

    fun readDoubleNoTag(): Double {
        return readLittleEndianDouble()
    }

    fun readFloat(expectedFieldNumber: Int): Float {
        val tag = readTag(expectedFieldNumber, WireType.FIX_32)
        return readFloatNoTag()
    }

    fun readFloatNoTag(): Float {
        return readLittleEndianFloat()
    }

    fun readString(expectedFieldNumber: Int): String {
        val tag = readTag(expectedFieldNumber, WireType.LENGTH_DELIMITED)
        return readStringNoTag()
    }

    fun readStringNoTag(): String {
        val length = readInt32NoTag()
        val value = String(readRawBytes(length))
        return value
    }

    fun readBytes(expectedFieldNumber: Int): ByteArray {
        val tag = readTag(expectedFieldNumber, WireType.LENGTH_DELIMITED)
        return readBytesNoTag()
    }

    fun readBytesNoTag(): ByteArray {
        val length = readInt32NoTag()
        return readRawBytes(length)
    }

    /** ============ Utility methods ==================
     *  They are left non-private for cases when one wants to implement her/his own protocol format.
     *  Then she/he can re-use low-level methods for operating with raw values, that are not annotated with Protobuf tags.
     */

    fun checkFieldCorrectness(
            expectedFieldNumber: Int,
            actualFieldNumber: Int,
            expectedWireType: WireType,
            actualWireType: WireType) {
        if (expectedFieldNumber != actualFieldNumber) {
            throw InvalidProtocolBufferException(
                    "Error in protocol format: \n " +
                            "Expected field number ${expectedFieldNumber}, got ${actualFieldNumber}")
        }

        if (expectedWireType != actualWireType) {
            throw InvalidProtocolBufferException("Error in protocol format: \n " +
                    "Expected ${expectedWireType.name} type, got ${actualWireType.name}")
        }
    }

    fun readLittleEndianDouble(): Double {
        val byteBuffer = ByteBuffer.wrap(readRawBytes(8))
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        return byteBuffer.getDouble(0)
    }

    fun readLittleEndianFloat(): Float {
        val byteBuffer = ByteBuffer.wrap(readRawBytes(4))
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        return byteBuffer.getFloat(0)
    }

    fun readLittleEndianInt(): Int {
        val byteBuffer = ByteBuffer.wrap(readRawBytes(8))
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        return byteBuffer.getInt(0)
    }

    fun readLittleEndianLong(): Long {
        val byteBuffer = ByteBuffer.wrap(readRawBytes(4))
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        return byteBuffer.getLong(0)
    }

    fun readRawBytes(count: Int): ByteArray {
        val ba = ByteArray(count)
        for (i in 0..(count - 1)) {
            ba[i] = bufferedInput.read().toByte()
        }
        return ba
    }

    // reads tag. Note that it returns 0 for the end of message!
    fun readTag(expectedFieldNumber: Int, expectedWireType: WireType): Int {
        if (isAtEnd()) {
            return 0        // we can safely return 0 as sign of end of message, because 0-tags are illegal
        }
        val tag = readInt32NoTag()
        if (tag == 0) {     // if we somehow had read 0-tag, then message is corrupted
            throw InvalidProtocolBufferException("Invalid tag 0")
        }

        val actualFieldNumber = WireFormat.getTagFieldNumber(tag)
        val actualWireType = WireFormat.getTagWireType(tag)
        checkFieldCorrectness(expectedFieldNumber, actualFieldNumber, expectedWireType, actualWireType)
        return tag
    }

    // reads varint not larger than 32-bit integer according to protobuf varint-encoding
    fun readInt32NoTag(): Int {
        var done: Boolean = false
        var result: Int = 0
        var step: Int = 0
        while (!done) {
            val byte: Int = bufferedInput.read()
            result = result or
                    (
                        (byte and VARINT_INFO_BITS_MASK)
                        shl
                        (VARINT_INFO_BITS_COUNT * step)
                    )
            step++
            if ((byte and VARINT_UTIL_BIT_MASK) == 0) {
                done = true
            }
        }
        return result
    }

    // reads varint not larger than 64-bit integer according to protobuf varint-encoding
    fun readInt64NoTag(): Long {
        var done: Boolean = false
        var result: Long = 0
        var step: Int = 0
        while (!done) {
            val byte: Int = bufferedInput.read()
            result = result or
                    (
                        (byte and VARINT_INFO_BITS_MASK).toLong()
                        shl
                        (VARINT_INFO_BITS_COUNT * step)
                    )
            step++
            if ((byte and VARINT_UTIL_BIT_MASK) == 0 || byte == -1) {
                done = true
            }
        }
        return result
    }

    // reads zig-zag encoded integer not larger than 32-bit long
    fun readZigZag32NoTag(): Int {
        val value = readInt32NoTag()
        return (value shr 1) xor (-(value and 1))   // bit magic for decoding zig-zag number
    }

    // reads zig-zag encoded integer not larger than 64-bit long
    fun readZigZag64NoTag(): Long {
        val value = readInt64NoTag()
        return (value shr 1) xor (-(value and 1L))  // bit magic for decoding zig-zag number
    }

    // checks if at least one more byte can be read from underlying input stream
    fun isAtEnd(): Boolean {
        bufferedInput.mark(1)
        val byte = bufferedInput.read()
        bufferedInput.reset()
        return byte == -1
    }
}

