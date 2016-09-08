class MessageZigZag private constructor (var int: Int, var long: Long) {
    //========== Properties ===========
    //sint32 int = 1

    //sint64 long = 2

    var errorCode: Int = 0

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
        //sint32 int = 1
        if (int != 0) {
            output.writeSInt32 (1, int)
        }

        //sint64 long = 2
        if (long != 0L) {
            output.writeSInt64 (2, long)
        }

    }

    fun mergeWith (other: MessageZigZag) {
        int = other.int
        long = other.long
        this.errorCode = other.errorCode
    }

    fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
        val builder = MessageZigZag.BuilderMessageZigZag(0, 0L)
        mergeWith(builder.parseFromWithSize(input, expectedSize).build())
    }

    fun mergeFrom (input: CodedInputStream) {
        val builder = MessageZigZag.BuilderMessageZigZag(0, 0L)
        mergeWith(builder.parseFrom(input).build())
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
        var size = 0
        if (int != 0) {
            size += WireFormat.getSInt32Size(1, int)
        }
        if (long != 0L) {
            size += WireFormat.getSInt64Size(2, long)
        }
        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
        return size
    }

    fun getSizeNoTag(): Int {
        var size = 0
        if (int != 0) {
            size += WireFormat.getSInt32Size(1, int)
        }
        if (long != 0L) {
            size += WireFormat.getSInt64Size(2, long)
        }
        return size
    }

    //========== Builder ===========
    class BuilderMessageZigZag constructor (var int: Int, var long: Long) {
        //========== Properties ===========
        //sint32 int = 1
        fun setInt(value: Int): MessageZigZag.BuilderMessageZigZag {
            int = value
            return this
        }

        //sint64 long = 2
        fun setLong(value: Long): MessageZigZag.BuilderMessageZigZag {
            long = value
            return this
        }

        var errorCode: Int = 0

        //========== Serialization methods ===========
        fun writeTo (output: CodedOutputStream) {
            //sint32 int = 1
            if (int != 0) {
                output.writeSInt32 (1, int)
            }

            //sint64 long = 2
            if (long != 0L) {
                output.writeSInt64 (2, long)
            }

        }

        //========== Mutating methods ===========
        fun build(): MessageZigZag {
            val res = MessageZigZag(int, long)
            res.errorCode = errorCode
            return res
        }

        fun parseFieldFrom(input: CodedInputStream): Boolean {
            if (input.isAtEnd()) { return false }
            val tag = input.readInt32NoTag()
            if (tag == 0) { return false }
            val fieldNumber = WireFormat.getTagFieldNumber(tag)
            val wireType = WireFormat.getTagWireType(tag)
            when(fieldNumber) {
                1 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    int = input.readSInt32NoTag()
                }
                2 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    long = input.readSInt64NoTag()
                }
                else -> errorCode = 4
            }
            return true
        }

        fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): MessageZigZag.BuilderMessageZigZag {
            while(getSizeNoTag() < expectedSize) {
                parseFieldFrom(input)
            }
            if (getSizeNoTag() > expectedSize) { errorCode = 2 }
            return this
        }

        fun parseFrom(input: CodedInputStream): MessageZigZag.BuilderMessageZigZag {
            while(parseFieldFrom(input)) {}
            return this
        }

        //========== Size-related methods ===========
        fun getSize(fieldNumber: Int): Int {
            var size = 0
            if (int != 0) {
                size += WireFormat.getSInt32Size(1, int)
            }
            if (long != 0L) {
                size += WireFormat.getSInt64Size(2, long)
            }
            size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
            return size
        }

        fun getSizeNoTag(): Int {
            var size = 0
            if (int != 0) {
                size += WireFormat.getSInt32Size(1, int)
            }
            if (long != 0L) {
                size += WireFormat.getSInt64Size(2, long)
            }
            return size
        }

    }

}

fun compareZigZags(kt1: MessageZigZag, kt2: MessageZigZag): Boolean {
    return (kt1.int == kt2.int) and (kt1.long == kt2.long)
}

fun checkZigZagSerializationIdentity(msg: MessageZigZag): Int {
    val outs = CodedOutputStream(ByteArray(msg.getSizeNoTag()))
    msg.writeTo(outs)

    val ins = CodedInputStream(outs.buffer)
    val readMsg = MessageZigZag.BuilderMessageZigZag(0, 0L).parseFrom(ins).build()

    if (readMsg.errorCode != 0) {
        return 1
    }
    if (compareZigZags(msg, readMsg)) {
        return 0
    } else {
        return 1
    }
}

fun testZigZagDefaultValues(): Int {
    val msg = MessageZigZag.BuilderMessageZigZag(0, 0L).build()
    return checkZigZagSerializationIdentity(msg)
}

fun testZigZagRandomValues(): Int {
    val msg = MessageZigZag.BuilderMessageZigZag(321782363, -466328364782634782L).build()
    return checkZigZagSerializationIdentity(msg)
}

fun testZigZagNegativeValues(): Int {
    val msg = MessageZigZag.BuilderMessageZigZag(-1321782363, -323721963313L).build()
    return checkZigZagSerializationIdentity(msg)
}

fun testZigZagPositiveValues(): Int {
    val msg = MessageZigZag.BuilderMessageZigZag(4788923, 240384293674L).build()
    return checkZigZagSerializationIdentity(msg)
}

fun testZigZagMaxValues(): Int {
    val msg = MessageZigZag.BuilderMessageZigZag(2147483647, 9223372036854775807L).build()
    return checkZigZagSerializationIdentity(msg)
}

fun testZigZagMinValues(): Int {
    val msg = MessageZigZag.BuilderMessageZigZag(-2147483647, -9223372036854775807L).build()
    return checkZigZagSerializationIdentity(msg)
}