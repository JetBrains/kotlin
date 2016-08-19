class DebugRequest private constructor (var type: DebugRequest.Type) {
  //========== Properties ===========
  //enum type = 1

  var errorCode: Int = 0

  //========== Nested enums declarations ===========
  enum class Type(val id: Int) {
    MEMORY_STATS (0),
    Unexpected(1);

    companion object {
      fun fromIntToType (ord: Int): Type {
        return when (ord) {
          0 -> Type.MEMORY_STATS
          else -> Unexpected
        }
      }
    }
  }
  //========== Serialization methods ===========
  fun writeTo (output: CodedOutputStream) {
    //enum type = 1
    if (type.id != DebugRequest.Type.fromIntToType(0).id) {
      output.writeEnum (1, type.id)
    }

  }

  fun mergeWith (other: DebugRequest) {
    type = other.type
    this.errorCode = other.errorCode
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = DebugRequest.BuilderDebugRequest(DebugRequest.Type.fromIntToType(0))
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())
  }

  fun mergeFrom (input: CodedInputStream) {
    val builder = DebugRequest.BuilderDebugRequest(DebugRequest.Type.fromIntToType(0))
    mergeWith(builder.parseFrom(input).build())
  }

  //========== Size-related methods ===========
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (type != DebugRequest.Type.fromIntToType(0)) {
      size += WireFormat.getEnumSize(1, type.id)
    }
    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
    return size
  }

  fun getSizeNoTag(): Int {
    var size = 0
    if (type != DebugRequest.Type.fromIntToType(0)) {
      size += WireFormat.getEnumSize(1, type.id)
    }
    return size
  }

  //========== Builder ===========
  class BuilderDebugRequest constructor (var type: DebugRequest.Type) {
    //========== Properties ===========
    //enum type = 1
    fun setType(value: DebugRequest.Type): DebugRequest.BuilderDebugRequest {
      type = value
      return this
    }

    var errorCode: Int = 0

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
      //enum type = 1
      if (type.id != DebugRequest.Type.fromIntToType(0).id) {
        output.writeEnum (1, type.id)
      }

    }

    //========== Mutating methods ===========
    fun build(): DebugRequest {
      val res = DebugRequest(type)
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
          type = DebugRequest.Type.fromIntToType(input.readEnumNoTag())
        }
        else -> errorCode = 4
      }
      return true
    }

    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): DebugRequest.BuilderDebugRequest {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { errorCode = 2 }
      return this
    }

    fun parseFrom(input: CodedInputStream): DebugRequest.BuilderDebugRequest {
      while(parseFieldFrom(input)) {}
      return this
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (type != DebugRequest.Type.fromIntToType(0)) {
        size += WireFormat.getEnumSize(1, type.id)
      }
      size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
      return size
    }

    fun getSizeNoTag(): Int {
      var size = 0
      if (type != DebugRequest.Type.fromIntToType(0)) {
        size += WireFormat.getEnumSize(1, type.id)
      }
      return size
    }

  }

}


class DebugResponseMemoryStats private constructor (var heapDynamicTail: Int, var heapStaticTail: Int, var heapDynamicMaxBytes: Int, var heapDynamicTotalBytes: Int) {
  //========== Properties ===========
  //int32 heapDynamicTail = 1

  //int32 heapStaticTail = 2

  //int32 heapDynamicMaxBytes = 3

  //int32 heapDynamicTotalBytes = 4

  var errorCode: Int = 0

  //========== Serialization methods ===========
  fun writeTo (output: CodedOutputStream) {
    //int32 heapDynamicTail = 1
    if (heapDynamicTail != 0) {
      output.writeInt32 (1, heapDynamicTail)
    }

    //int32 heapStaticTail = 2
    if (heapStaticTail != 0) {
      output.writeInt32 (2, heapStaticTail)
    }

    //int32 heapDynamicMaxBytes = 3
    if (heapDynamicMaxBytes != 0) {
      output.writeInt32 (3, heapDynamicMaxBytes)
    }

    //int32 heapDynamicTotalBytes = 4
    if (heapDynamicTotalBytes != 0) {
      output.writeInt32 (4, heapDynamicTotalBytes)
    }

  }

  fun mergeWith (other: DebugResponseMemoryStats) {
    heapDynamicTail = other.heapDynamicTail
    heapStaticTail = other.heapStaticTail
    heapDynamicMaxBytes = other.heapDynamicMaxBytes
    heapDynamicTotalBytes = other.heapDynamicTotalBytes
    this.errorCode = other.errorCode
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = DebugResponseMemoryStats.BuilderDebugResponseMemoryStats(0, 0, 0, 0)
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())
  }

  fun mergeFrom (input: CodedInputStream) {
    val builder = DebugResponseMemoryStats.BuilderDebugResponseMemoryStats(0, 0, 0, 0)
    mergeWith(builder.parseFrom(input).build())
  }

  //========== Size-related methods ===========
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (heapDynamicTail != 0) {
      size += WireFormat.getInt32Size(1, heapDynamicTail)
    }
    if (heapStaticTail != 0) {
      size += WireFormat.getInt32Size(2, heapStaticTail)
    }
    if (heapDynamicMaxBytes != 0) {
      size += WireFormat.getInt32Size(3, heapDynamicMaxBytes)
    }
    if (heapDynamicTotalBytes != 0) {
      size += WireFormat.getInt32Size(4, heapDynamicTotalBytes)
    }
    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
    return size
  }

  fun getSizeNoTag(): Int {
    var size = 0
    if (heapDynamicTail != 0) {
      size += WireFormat.getInt32Size(1, heapDynamicTail)
    }
    if (heapStaticTail != 0) {
      size += WireFormat.getInt32Size(2, heapStaticTail)
    }
    if (heapDynamicMaxBytes != 0) {
      size += WireFormat.getInt32Size(3, heapDynamicMaxBytes)
    }
    if (heapDynamicTotalBytes != 0) {
      size += WireFormat.getInt32Size(4, heapDynamicTotalBytes)
    }
    return size
  }

  //========== Builder ===========
  class BuilderDebugResponseMemoryStats constructor (var heapDynamicTail: Int, var heapStaticTail: Int, var heapDynamicMaxBytes: Int, var heapDynamicTotalBytes: Int) {
    //========== Properties ===========
    //int32 heapDynamicTail = 1
    fun setHeapDynamicTail(value: Int): DebugResponseMemoryStats.BuilderDebugResponseMemoryStats {
      heapDynamicTail = value
      return this
    }

    //int32 heapStaticTail = 2
    fun setHeapStaticTail(value: Int): DebugResponseMemoryStats.BuilderDebugResponseMemoryStats {
      heapStaticTail = value
      return this
    }

    //int32 heapDynamicMaxBytes = 3
    fun setHeapDynamicMaxBytes(value: Int): DebugResponseMemoryStats.BuilderDebugResponseMemoryStats {
      heapDynamicMaxBytes = value
      return this
    }

    //int32 heapDynamicTotalBytes = 4
    fun setHeapDynamicTotalBytes(value: Int): DebugResponseMemoryStats.BuilderDebugResponseMemoryStats {
      heapDynamicTotalBytes = value
      return this
    }

    var errorCode: Int = 0

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
      //int32 heapDynamicTail = 1
      if (heapDynamicTail != 0) {
        output.writeInt32 (1, heapDynamicTail)
      }

      //int32 heapStaticTail = 2
      if (heapStaticTail != 0) {
        output.writeInt32 (2, heapStaticTail)
      }

      //int32 heapDynamicMaxBytes = 3
      if (heapDynamicMaxBytes != 0) {
        output.writeInt32 (3, heapDynamicMaxBytes)
      }

      //int32 heapDynamicTotalBytes = 4
      if (heapDynamicTotalBytes != 0) {
        output.writeInt32 (4, heapDynamicTotalBytes)
      }

    }

    //========== Mutating methods ===========
    fun build(): DebugResponseMemoryStats {
      val res = DebugResponseMemoryStats(heapDynamicTail, heapStaticTail, heapDynamicMaxBytes, heapDynamicTotalBytes)
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
          heapDynamicTail = input.readInt32NoTag()
        }
        2 -> {
          if (wireType.id != WireType.VARINT.id) {
            errorCode = 1
            return false
          }
          heapStaticTail = input.readInt32NoTag()
        }
        3 -> {
          if (wireType.id != WireType.VARINT.id) {
            errorCode = 1
            return false
          }
          heapDynamicMaxBytes = input.readInt32NoTag()
        }
        4 -> {
          if (wireType.id != WireType.VARINT.id) {
            errorCode = 1
            return false
          }
          heapDynamicTotalBytes = input.readInt32NoTag()
        }
        else -> errorCode = 4
      }
      return true
    }

    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): DebugResponseMemoryStats.BuilderDebugResponseMemoryStats {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { errorCode = 2 }
      return this
    }

    fun parseFrom(input: CodedInputStream): DebugResponseMemoryStats.BuilderDebugResponseMemoryStats {
      while(parseFieldFrom(input)) {}
      return this
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (heapDynamicTail != 0) {
        size += WireFormat.getInt32Size(1, heapDynamicTail)
      }
      if (heapStaticTail != 0) {
        size += WireFormat.getInt32Size(2, heapStaticTail)
      }
      if (heapDynamicMaxBytes != 0) {
        size += WireFormat.getInt32Size(3, heapDynamicMaxBytes)
      }
      if (heapDynamicTotalBytes != 0) {
        size += WireFormat.getInt32Size(4, heapDynamicTotalBytes)
      }
      size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
      return size
    }

    fun getSizeNoTag(): Int {
      var size = 0
      if (heapDynamicTail != 0) {
        size += WireFormat.getInt32Size(1, heapDynamicTail)
      }
      if (heapStaticTail != 0) {
        size += WireFormat.getInt32Size(2, heapStaticTail)
      }
      if (heapDynamicMaxBytes != 0) {
        size += WireFormat.getInt32Size(3, heapDynamicMaxBytes)
      }
      if (heapDynamicTotalBytes != 0) {
        size += WireFormat.getInt32Size(4, heapDynamicTotalBytes)
      }
      return size
    }

  }

}


