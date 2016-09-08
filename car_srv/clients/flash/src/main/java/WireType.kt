/**
 * Created by Dmitry Savvinov on 7/6/16.
 * Enum for possible WireTypes.
 * See details at [official Google reference]()https://developers.google.com/protocol-buffers/docs/encoding#structure)
 */

enum class WireType(val id: Int) {
    VARINT(0),              // int32, int64, uint32, uint64, sint32, sint64, bool, enum
    FIX_64(1),              // fixed64, sfixed64, double
    LENGTH_DELIMITED(2),    // string, bytes, embedded messages, packed repeated fields
    START_GROUP(3),         // groups (deprecated)
    END_GROUP(4),           // groups (deprecated)
    FIX_32(5);              // fixed32, sfixed32, float

    companion object {
        infix fun from (value: Byte): WireType {
            return when (value) {
                0.toByte() -> VARINT
                1.toByte() -> FIX_64
                2.toByte() -> LENGTH_DELIMITED
                3.toByte() -> START_GROUP
                4.toByte() -> END_GROUP
                5.toByte() -> FIX_32
                else -> throw IllegalArgumentException()
            }
        }
    }
}