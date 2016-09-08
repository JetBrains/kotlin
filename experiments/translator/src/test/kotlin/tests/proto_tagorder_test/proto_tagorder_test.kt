class TagOrder private constructor (var sint: Int, var int: Int, var slong_array: LongArray, var enum_field: TagOrder.Enum) {
    //========== Properties ===========
    //sint32 sint = 8

    //int32 int = 3423

    //repeated sint64 slong_array = 12321

    //enum enum_field = 938423

    var errorCode: Int = 0

    //========== Nested enums declarations ===========
    enum class Enum(val id: Int) {
        first_val (0),
        second_val (1),
        Unexpected(2);

        companion object {
            fun fromIntToEnum (ord: Int): Enum {
                return when (ord) {
                    0 -> Enum.first_val
                    1 -> Enum.second_val
                    else -> Unexpected
                }
            }
        }
    }
    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
        //sint32 sint = 8
        if (sint != 0) {
            output.writeSInt32 (8, sint)
        }

        //int32 int = 3423
        if (int != 0) {
            output.writeInt32 (3423, int)
        }

        //repeated sint64 slong_array = 12321
        if (slong_array.size > 0) {
            output.writeTag(12321, WireType.LENGTH_DELIMITED)
            var arrayByteSize = 0

            if (slong_array.size != 0) {
                do {
                    var arraySize = 0
                    var i = 0
                    while (i < slong_array.size) {
                        arraySize += WireFormat.getSInt64SizeNoTag(slong_array[i])
                        i += 1
                    }
                    arrayByteSize += arraySize
                } while(false)
            }
            output.writeInt32NoTag(arrayByteSize)

            do {
                var i = 0
                while (i < slong_array.size) {
                    output.writeSInt64NoTag (slong_array[i])
                    i += 1
                }
            } while(false)
        }

        //enum enum_field = 938423
        if (enum_field.id != TagOrder.Enum.fromIntToEnum(0).id) {
            output.writeEnum (938423, enum_field.id)
        }

    }

    fun mergeWith (other: TagOrder) {
        sint = other.sint
        int = other.int
        slong_array = slong_array.plus((other.slong_array))
        enum_field = other.enum_field
        this.errorCode = other.errorCode
    }

    fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
        val builder = TagOrder.BuilderTagOrder(0, 0, LongArray(0), TagOrder.Enum.fromIntToEnum(0))
        mergeWith(builder.parseFromWithSize(input, expectedSize).build())
    }

    fun mergeFrom (input: CodedInputStream) {
        val builder = TagOrder.BuilderTagOrder(0, 0, LongArray(0), TagOrder.Enum.fromIntToEnum(0))
        mergeWith(builder.parseFrom(input).build())
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
        var size = 0
        if (sint != 0) {
            size += WireFormat.getSInt32Size(8, sint)
        }
        if (int != 0) {
            size += WireFormat.getInt32Size(3423, int)
        }
        if (slong_array.size != 0) {
            do {
                var arraySize = 0
                size += WireFormat.getTagSize(12321, WireType.LENGTH_DELIMITED)
                var i = 0
                while (i < slong_array.size) {
                    arraySize += WireFormat.getSInt64SizeNoTag(slong_array[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        if (enum_field != TagOrder.Enum.fromIntToEnum(0)) {
            size += WireFormat.getEnumSize(938423, enum_field.id)
        }
        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
        return size
    }

    fun getSizeNoTag(): Int {
        var size = 0
        if (sint != 0) {
            size += WireFormat.getSInt32Size(8, sint)
        }
        if (int != 0) {
            size += WireFormat.getInt32Size(3423, int)
        }
        if (slong_array.size != 0) {
            do {
                var arraySize = 0
                size += WireFormat.getTagSize(12321, WireType.LENGTH_DELIMITED)
                var i = 0
                while (i < slong_array.size) {
                    arraySize += WireFormat.getSInt64SizeNoTag(slong_array[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        if (enum_field != TagOrder.Enum.fromIntToEnum(0)) {
            size += WireFormat.getEnumSize(938423, enum_field.id)
        }
        return size
    }

    //========== Builder ===========
    class BuilderTagOrder constructor (var sint: Int, var int: Int, var slong_array: LongArray, var enum_field: TagOrder.Enum) {
        //========== Properties ===========
        //sint32 sint = 8
        fun setSint(value: Int): TagOrder.BuilderTagOrder {
            sint = value
            return this
        }

        //int32 int = 3423
        fun setInt(value: Int): TagOrder.BuilderTagOrder {
            int = value
            return this
        }

        //repeated sint64 slong_array = 12321
        fun setSlong_array(value: LongArray): TagOrder.BuilderTagOrder {
            slong_array = value
            return this
        }
        fun setslong_arrayByIndex(index: Int, value: Long): TagOrder.BuilderTagOrder {
            slong_array[index] = value
            return this
        }

        //enum enum_field = 938423
        fun setEnum_field(value: TagOrder.Enum): TagOrder.BuilderTagOrder {
            enum_field = value
            return this
        }

        var errorCode: Int = 0

        //========== Serialization methods ===========
        fun writeTo (output: CodedOutputStream) {
            //sint32 sint = 8
            if (sint != 0) {
                output.writeSInt32 (8, sint)
            }

            //int32 int = 3423
            if (int != 0) {
                output.writeInt32 (3423, int)
            }

            //repeated sint64 slong_array = 12321
            if (slong_array.size > 0) {
                output.writeTag(12321, WireType.LENGTH_DELIMITED)
                var arrayByteSize = 0

                if (slong_array.size != 0) {
                    do {
                        var arraySize = 0
                        var i = 0
                        while (i < slong_array.size) {
                            arraySize += WireFormat.getSInt64SizeNoTag(slong_array[i])
                            i += 1
                        }
                        arrayByteSize += arraySize
                    } while(false)
                }
                output.writeInt32NoTag(arrayByteSize)

                do {
                    var i = 0
                    while (i < slong_array.size) {
                        output.writeSInt64NoTag (slong_array[i])
                        i += 1
                    }
                } while(false)
            }

            //enum enum_field = 938423
            if (enum_field.id != TagOrder.Enum.fromIntToEnum(0).id) {
                output.writeEnum (938423, enum_field.id)
            }

        }

        //========== Mutating methods ===========
        fun build(): TagOrder {
            val res = TagOrder(sint, int, slong_array, enum_field)
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
                8 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    sint = input.readSInt32NoTag()
                }
                3423 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    int = input.readInt32NoTag()
                }
                12321 -> {
                    if (wireType.id != WireType.LENGTH_DELIMITED.id) {
                        errorCode = 1
                        return false
                    }
                    val expectedByteSize = input.readInt32NoTag()
                    var newArray = LongArray(0)
                    var readSize = 0
                    do {
                        var i = 0
                        while(readSize < expectedByteSize) {
                            val tmp = LongArray(1)
                            tmp[0] = input.readSInt64NoTag()
                            newArray = newArray.plus(tmp)
                            readSize += WireFormat.getSInt64SizeNoTag(tmp[0])
                        }
                        slong_array = newArray
                    } while (false)
                }
                938423 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    enum_field = TagOrder.Enum.fromIntToEnum(input.readEnumNoTag())
                }
                else -> errorCode = 4
            }
            return true
        }

        fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): TagOrder.BuilderTagOrder {
            while(getSizeNoTag() < expectedSize) {
                parseFieldFrom(input)
            }
            if (getSizeNoTag() > expectedSize) { errorCode = 2 }
            return this
        }

        fun parseFrom(input: CodedInputStream): TagOrder.BuilderTagOrder {
            while(parseFieldFrom(input)) {}
            return this
        }

        //========== Size-related methods ===========
        fun getSize(fieldNumber: Int): Int {
            var size = 0
            if (sint != 0) {
                size += WireFormat.getSInt32Size(8, sint)
            }
            if (int != 0) {
                size += WireFormat.getInt32Size(3423, int)
            }
            if (slong_array.size != 0) {
                do {
                    var arraySize = 0
                    size += WireFormat.getTagSize(12321, WireType.LENGTH_DELIMITED)
                    var i = 0
                    while (i < slong_array.size) {
                        arraySize += WireFormat.getSInt64SizeNoTag(slong_array[i])
                        i += 1
                    }
                    size += arraySize
                    size += WireFormat.getInt32SizeNoTag(arraySize)
                } while(false)
            }
            if (enum_field != TagOrder.Enum.fromIntToEnum(0)) {
                size += WireFormat.getEnumSize(938423, enum_field.id)
            }
            size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
            return size
        }

        fun getSizeNoTag(): Int {
            var size = 0
            if (sint != 0) {
                size += WireFormat.getSInt32Size(8, sint)
            }
            if (int != 0) {
                size += WireFormat.getInt32Size(3423, int)
            }
            if (slong_array.size != 0) {
                do {
                    var arraySize = 0
                    size += WireFormat.getTagSize(12321, WireType.LENGTH_DELIMITED)
                    var i = 0
                    while (i < slong_array.size) {
                        arraySize += WireFormat.getSInt64SizeNoTag(slong_array[i])
                        i += 1
                    }
                    size += arraySize
                    size += WireFormat.getInt32SizeNoTag(arraySize)
                } while(false)
            }
            if (enum_field != TagOrder.Enum.fromIntToEnum(0)) {
                size += WireFormat.getEnumSize(938423, enum_field.id)
            }
            return size
        }

    }

}

class TagOrderShuffled private constructor (var sint: Int, var int: Int, var slong_array: LongArray, var enum_field: TagOrderShuffled.Enum) {
    //========== Properties ===========
    //sint32 sint = 8

    //int32 int = 3423

    //repeated sint64 slong_array = 12321

    //enum enum_field = 938423

    var errorCode: Int = 0

    //========== Nested enums declarations ===========
    enum class Enum(val id: Int) {
        first_val (0),
        second_val (1),
        Unexpected(2);

        companion object {
            fun fromIntToEnum (ord: Int): Enum {
                return when (ord) {
                    0 -> Enum.first_val
                    1 -> Enum.second_val
                    else -> Unexpected
                }
            }
        }
    }
    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
        //sint32 sint = 8
        if (sint != 0) {
            output.writeSInt32 (8, sint)
        }

        //int32 int = 3423
        if (int != 0) {
            output.writeInt32 (3423, int)
        }

        //repeated sint64 slong_array = 12321
        if (slong_array.size > 0) {
            output.writeTag(12321, WireType.LENGTH_DELIMITED)
            var arrayByteSize = 0

            if (slong_array.size != 0) {
                do {
                    var arraySize = 0
                    var i = 0
                    while (i < slong_array.size) {
                        arraySize += WireFormat.getSInt64SizeNoTag(slong_array[i])
                        i += 1
                    }
                    arrayByteSize += arraySize
                } while(false)
            }
            output.writeInt32NoTag(arrayByteSize)

            do {
                var i = 0
                while (i < slong_array.size) {
                    output.writeSInt64NoTag (slong_array[i])
                    i += 1
                }
            } while(false)
        }

        //enum enum_field = 938423
        if (enum_field.id != TagOrderShuffled.Enum.fromIntToEnum(0).id) {
            output.writeEnum (938423, enum_field.id)
        }

    }

    fun mergeWith (other: TagOrderShuffled) {
        sint = other.sint
        int = other.int
        slong_array = slong_array.plus((other.slong_array))
        enum_field = other.enum_field
        this.errorCode = other.errorCode
    }

    fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
        val builder = TagOrderShuffled.BuilderTagOrderShuffled(0, 0, LongArray(0), TagOrderShuffled.Enum.fromIntToEnum(0))
        mergeWith(builder.parseFromWithSize(input, expectedSize).build())
    }

    fun mergeFrom (input: CodedInputStream) {
        val builder = TagOrderShuffled.BuilderTagOrderShuffled(0, 0, LongArray(0), TagOrderShuffled.Enum.fromIntToEnum(0))
        mergeWith(builder.parseFrom(input).build())
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
        var size = 0
        if (sint != 0) {
            size += WireFormat.getSInt32Size(8, sint)
        }
        if (int != 0) {
            size += WireFormat.getInt32Size(3423, int)
        }
        if (slong_array.size != 0) {
            do {
                var arraySize = 0
                size += WireFormat.getTagSize(12321, WireType.LENGTH_DELIMITED)
                var i = 0
                while (i < slong_array.size) {
                    arraySize += WireFormat.getSInt64SizeNoTag(slong_array[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        if (enum_field != TagOrderShuffled.Enum.fromIntToEnum(0)) {
            size += WireFormat.getEnumSize(938423, enum_field.id)
        }
        size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
        return size
    }

    fun getSizeNoTag(): Int {
        var size = 0
        if (sint != 0) {
            size += WireFormat.getSInt32Size(8, sint)
        }
        if (int != 0) {
            size += WireFormat.getInt32Size(3423, int)
        }
        if (slong_array.size != 0) {
            do {
                var arraySize = 0
                size += WireFormat.getTagSize(12321, WireType.LENGTH_DELIMITED)
                var i = 0
                while (i < slong_array.size) {
                    arraySize += WireFormat.getSInt64SizeNoTag(slong_array[i])
                    i += 1
                }
                size += arraySize
                size += WireFormat.getInt32SizeNoTag(arraySize)
            } while(false)
        }
        if (enum_field != TagOrderShuffled.Enum.fromIntToEnum(0)) {
            size += WireFormat.getEnumSize(938423, enum_field.id)
        }
        return size
    }

    //========== Builder ===========
    class BuilderTagOrderShuffled constructor (var sint: Int, var int: Int, var slong_array: LongArray, var enum_field: TagOrderShuffled.Enum) {
        //========== Properties ===========
        //sint32 sint = 8
        fun setSint(value: Int): TagOrderShuffled.BuilderTagOrderShuffled {
            sint = value
            return this
        }

        //int32 int = 3423
        fun setInt(value: Int): TagOrderShuffled.BuilderTagOrderShuffled {
            int = value
            return this
        }

        //repeated sint64 slong_array = 12321
        fun setSlong_array(value: LongArray): TagOrderShuffled.BuilderTagOrderShuffled {
            slong_array = value
            return this
        }
        fun setslong_arrayByIndex(index: Int, value: Long): TagOrderShuffled.BuilderTagOrderShuffled {
            slong_array[index] = value
            return this
        }

        //enum enum_field = 938423
        fun setEnum_field(value: TagOrderShuffled.Enum): TagOrderShuffled.BuilderTagOrderShuffled {
            enum_field = value
            return this
        }

        var errorCode: Int = 0

        //========== Serialization methods ===========
        fun writeTo (output: CodedOutputStream) {
            //sint32 sint = 8
            if (sint != 0) {
                output.writeSInt32 (8, sint)
            }

            //int32 int = 3423
            if (int != 0) {
                output.writeInt32 (3423, int)
            }

            //repeated sint64 slong_array = 12321
            if (slong_array.size > 0) {
                output.writeTag(12321, WireType.LENGTH_DELIMITED)
                var arrayByteSize = 0

                if (slong_array.size != 0) {
                    do {
                        var arraySize = 0
                        var i = 0
                        while (i < slong_array.size) {
                            arraySize += WireFormat.getSInt64SizeNoTag(slong_array[i])
                            i += 1
                        }
                        arrayByteSize += arraySize
                    } while(false)
                }
                output.writeInt32NoTag(arrayByteSize)

                do {
                    var i = 0
                    while (i < slong_array.size) {
                        output.writeSInt64NoTag (slong_array[i])
                        i += 1
                    }
                } while(false)
            }

            //enum enum_field = 938423
            if (enum_field.id != TagOrderShuffled.Enum.fromIntToEnum(0).id) {
                output.writeEnum (938423, enum_field.id)
            }

        }

        //========== Mutating methods ===========
        fun build(): TagOrderShuffled {
            val res = TagOrderShuffled(sint, int, slong_array, enum_field)
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
                8 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    sint = input.readSInt32NoTag()
                }
                3423 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    int = input.readInt32NoTag()
                }
                12321 -> {
                    if (wireType.id != WireType.LENGTH_DELIMITED.id) {
                        errorCode = 1
                        return false
                    }
                    val expectedByteSize = input.readInt32NoTag()
                    var newArray = LongArray(0)
                    var readSize = 0
                    do {
                        var i = 0
                        while(readSize < expectedByteSize) {
                            val tmp = LongArray(1)
                            tmp[0] = input.readSInt64NoTag()
                            newArray = newArray.plus(tmp)
                            readSize += WireFormat.getSInt64SizeNoTag(tmp[0])
                        }
                        slong_array = newArray
                    } while (false)
                }
                938423 -> {
                    if (wireType.id != WireType.VARINT.id) {
                        errorCode = 1
                        return false
                    }
                    enum_field = TagOrderShuffled.Enum.fromIntToEnum(input.readEnumNoTag())
                }
                else -> errorCode = 4
            }
            return true
        }

        fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): TagOrderShuffled.BuilderTagOrderShuffled {
            while(getSizeNoTag() < expectedSize) {
                parseFieldFrom(input)
            }
            if (getSizeNoTag() > expectedSize) { errorCode = 2 }
            return this
        }

        fun parseFrom(input: CodedInputStream): TagOrderShuffled.BuilderTagOrderShuffled {
            while(parseFieldFrom(input)) {}
            return this
        }

        //========== Size-related methods ===========
        fun getSize(fieldNumber: Int): Int {
            var size = 0
            if (sint != 0) {
                size += WireFormat.getSInt32Size(8, sint)
            }
            if (int != 0) {
                size += WireFormat.getInt32Size(3423, int)
            }
            if (slong_array.size != 0) {
                do {
                    var arraySize = 0
                    size += WireFormat.getTagSize(12321, WireType.LENGTH_DELIMITED)
                    var i = 0
                    while (i < slong_array.size) {
                        arraySize += WireFormat.getSInt64SizeNoTag(slong_array[i])
                        i += 1
                    }
                    size += arraySize
                    size += WireFormat.getInt32SizeNoTag(arraySize)
                } while(false)
            }
            if (enum_field != TagOrderShuffled.Enum.fromIntToEnum(0)) {
                size += WireFormat.getEnumSize(938423, enum_field.id)
            }
            size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
            return size
        }

        fun getSizeNoTag(): Int {
            var size = 0
            if (sint != 0) {
                size += WireFormat.getSInt32Size(8, sint)
            }
            if (int != 0) {
                size += WireFormat.getInt32Size(3423, int)
            }
            if (slong_array.size != 0) {
                do {
                    var arraySize = 0
                    size += WireFormat.getTagSize(12321, WireType.LENGTH_DELIMITED)
                    var i = 0
                    while (i < slong_array.size) {
                        arraySize += WireFormat.getSInt64SizeNoTag(slong_array[i])
                        i += 1
                    }
                    size += arraySize
                    size += WireFormat.getInt32SizeNoTag(arraySize)
                } while(false)
            }
            if (enum_field != TagOrderShuffled.Enum.fromIntToEnum(0)) {
                size += WireFormat.getEnumSize(938423, enum_field.id)
            }
            return size
        }

    }

}

fun compareLongArrays(lhs: LongArray, rhs: LongArray): Boolean {
    if (lhs.size != rhs.size) {
        return false
    }

    var i = 0
    while (i < lhs.size) {
        if (lhs[i] != rhs[i]) {
            return false
        }
        i += 1
    }
    return true
}

fun compareTagAndShuffledTag(kt1: TagOrder, kt2: TagOrderShuffled): Boolean {
    return (kt1.enum_field.id == kt2.enum_field.id) and
            (kt1.int == kt2.int) and
            (kt1.sint == kt2.sint) and
            compareLongArrays(kt1.slong_array, kt2.slong_array)
}

fun checkTagToShuffled(msg: TagOrder): Int {
    val outs = CodedOutputStream(ByteArray(msg.getSizeNoTag()))
    msg.writeTo(outs)

    val ins = CodedInputStream(outs.buffer)
    val readMsg = TagOrderShuffled.BuilderTagOrderShuffled(0, 0, LongArray(0), TagOrderShuffled.Enum.first_val).parseFrom(ins).build()

    if (compareTagAndShuffledTag(msg, readMsg)) {
        return 0
    } else {
        return 1
    }
}

fun checkShuffledToTag(msg: TagOrderShuffled): Int {
    val outs = CodedOutputStream(ByteArray(msg.getSizeNoTag()))
    msg.writeTo(outs)

    val ins = CodedInputStream(outs.buffer)
    val readMsg = TagOrder.BuilderTagOrder(0, 0, LongArray(0), TagOrder.Enum.first_val).parseFrom(ins).build()

    if (readMsg.errorCode != 0) {
        return 1
    }
    if (compareTagAndShuffledTag(readMsg, msg)) {
        return 0
    } else {
        return 1
    }
}

fun testTagToShuffled(): Int {
    val arr = LongArray(3)
    arr[0] = 94289342345678289L
    arr[1]  = -423643342423442342L

    val msg = TagOrder.BuilderTagOrder(312321, -34268, arr, TagOrder.Enum.first_val).build()
    return checkTagToShuffled(msg)
}

fun testShuffledToTag(): Int {
    val arr = LongArray(3)
    arr[0] = 94289342345678289L
    arr[1]  = -423643342423442342L

    val msg = TagOrderShuffled.BuilderTagOrderShuffled(312321, -34268, arr, TagOrderShuffled.Enum.first_val).build()
    return checkShuffledToTag(msg)
}