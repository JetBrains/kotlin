class TaskRequest private constructor (var type: TaskRequest.Type) {
  //========== Properties ===========
  //enum type = 1

  var errorCode: Int = 0

  //========== Nested enums declarations ===========
  enum class Type(val id: Int) {
    DEBUG (0),
    ROUTE (1),
    Unexpected(2);

    companion object {
      fun fromIntToType (ord: Int): Type {
        return when (ord) {
          0 -> Type.DEBUG
          1 -> Type.ROUTE
          else -> Unexpected
        }
      }
    }
  }
  //========== Serialization methods ===========
  fun writeTo (output: CodedOutputStream) {
    //enum type = 1
    if (type.id != TaskRequest.Type.fromIntToType(0).id) {
      output.writeEnum (1, type.id)
    }

  }

  fun mergeWith (other: TaskRequest) {
    type = other.type
    this.errorCode = other.errorCode
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = TaskRequest.BuilderTaskRequest(TaskRequest.Type.fromIntToType(0))
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())
  }

  fun mergeFrom (input: CodedInputStream) {
    val builder = TaskRequest.BuilderTaskRequest(TaskRequest.Type.fromIntToType(0))
    mergeWith(builder.parseFrom(input).build())
  }

  //========== Size-related methods ===========
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (type != TaskRequest.Type.fromIntToType(0)) {
      size += WireFormat.getEnumSize(1, type.id)
    }
    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
    return size
  }

  fun getSizeNoTag(): Int {
    var size = 0
    if (type != TaskRequest.Type.fromIntToType(0)) {
      size += WireFormat.getEnumSize(1, type.id)
    }
    return size
  }

  //========== Builder ===========
  class BuilderTaskRequest constructor (var type: TaskRequest.Type) {
    //========== Properties ===========
    //enum type = 1
    fun setType(value: TaskRequest.Type): TaskRequest.BuilderTaskRequest {
      type = value
      return this
    }

    var errorCode: Int = 0

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
      //enum type = 1
      if (type.id != TaskRequest.Type.fromIntToType(0).id) {
        output.writeEnum (1, type.id)
      }

    }

    //========== Mutating methods ===========
    fun build(): TaskRequest {
      val res = TaskRequest(type)
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
          type = TaskRequest.Type.fromIntToType(input.readEnumNoTag())
        }
        else -> errorCode = 4
      }
      return true
    }

    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): TaskRequest.BuilderTaskRequest {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { errorCode = 2 }
      return this
    }

    fun parseFrom(input: CodedInputStream): TaskRequest.BuilderTaskRequest {
      while(parseFieldFrom(input)) {}
      return this
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (type != TaskRequest.Type.fromIntToType(0)) {
        size += WireFormat.getEnumSize(1, type.id)
      }
      size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
      return size
    }

    fun getSizeNoTag(): Int {
      var size = 0
      if (type != TaskRequest.Type.fromIntToType(0)) {
        size += WireFormat.getEnumSize(1, type.id)
      }
      return size
    }

  }

}


