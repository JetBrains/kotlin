
class DirectionRequest private constructor (var command: DirectionRequest.Command, var sid: Int) {
  //========== Properties ===========
  //enum command = 1

  //int32 sid = 2

  var errorCode: Int = 0

  //========== Nested enums declarations ===========
  enum class Command(val id: Int) {
    stop (0),
    forward (1),
    backward (2),
    left (3),
    right (4),
    Unexpected(5);

    companion object {
      fun fromIntToCommand (ord: Int): Command {
        return when (ord) {
          0 -> Command.stop
          1 -> Command.forward
          2 -> Command.backward
          3 -> Command.left
          4 -> Command.right
          else -> Unexpected
        }
      }
    }
  }
  //========== Serialization methods ===========
  fun writeTo (output: CodedOutputStream) {
    //enum command = 1
    if (command.id != DirectionRequest.Command.fromIntToCommand(0).id) {
      output.writeEnum (1, command.id)
    }

    //int32 sid = 2
    if (sid != 0) {
      output.writeInt32 (2, sid)
    }

  }

  fun mergeWith (other: DirectionRequest) {
    command = other.command
    sid = other.sid
    this.errorCode = other.errorCode
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = DirectionRequest.BuilderDirectionRequest(DirectionRequest.Command.fromIntToCommand(0), 0)
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())
  }

  fun mergeFrom (input: CodedInputStream) {
    val builder = DirectionRequest.BuilderDirectionRequest(DirectionRequest.Command.fromIntToCommand(0), 0)
    mergeWith(builder.parseFrom(input).build())
  }

  //========== Size-related methods ===========
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (command != DirectionRequest.Command.fromIntToCommand(0)) {
      size += WireFormat.getEnumSize(1, command.id)
    }
    if (sid != 0) {
      size += WireFormat.getInt32Size(2, sid)
    }
    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
    return size
  }

  fun getSizeNoTag(): Int {
    var size = 0
    if (command != DirectionRequest.Command.fromIntToCommand(0)) {
      size += WireFormat.getEnumSize(1, command.id)
    }
    if (sid != 0) {
      size += WireFormat.getInt32Size(2, sid)
    }
    return size
  }

  //========== Builder ===========
  class BuilderDirectionRequest constructor (var command: DirectionRequest.Command, var sid: Int) {
    //========== Properties ===========
    //enum command = 1
    fun setCommand(value: DirectionRequest.Command): DirectionRequest.BuilderDirectionRequest {
      command = value
      return this
    }

    //int32 sid = 2
    fun setSid(value: Int): DirectionRequest.BuilderDirectionRequest {
      sid = value
      return this
    }

    var errorCode: Int = 0

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
      //enum command = 1
      if (command.id != DirectionRequest.Command.fromIntToCommand(0).id) {
        output.writeEnum (1, command.id)
      }

      //int32 sid = 2
      if (sid != 0) {
        output.writeInt32 (2, sid)
      }

    }

    //========== Mutating methods ===========
    fun build(): DirectionRequest {
      val res = DirectionRequest(command, sid)
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
          command = DirectionRequest.Command.fromIntToCommand(input.readEnumNoTag())
        }
        2 -> {
          if (wireType.id != WireType.VARINT.id) {
            errorCode = 1
            return false
          }
          sid = input.readInt32NoTag()
        }
        else -> errorCode = 4
      }
      return true
    }

    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): DirectionRequest.BuilderDirectionRequest {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { errorCode = 2 }
      return this
    }

    fun parseFrom(input: CodedInputStream): DirectionRequest.BuilderDirectionRequest {
      while(parseFieldFrom(input)) {}
      return this
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (command != DirectionRequest.Command.fromIntToCommand(0)) {
        size += WireFormat.getEnumSize(1, command.id)
      }
      if (sid != 0) {
        size += WireFormat.getInt32Size(2, sid)
      }
      size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
      return size
    }

    fun getSizeNoTag(): Int {
      var size = 0
      if (command != DirectionRequest.Command.fromIntToCommand(0)) {
        size += WireFormat.getEnumSize(1, command.id)
      }
      if (sid != 0) {
        size += WireFormat.getInt32Size(2, sid)
      }
      return size
    }

  }

}

class DirectionResponse private constructor (var code: Int) {
  //========== Properties ===========
  //int32 code = 1

  var errorCode: Int = 0

  //========== Serialization methods ===========
  fun writeTo (output: CodedOutputStream) {
    //int32 code = 1
    if (code != 0) {
      output.writeInt32 (1, code)
    }

  }

  fun mergeWith (other: DirectionResponse) {
    code = other.code
    this.errorCode = other.errorCode
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = DirectionResponse.BuilderDirectionResponse(0)
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())
  }

  fun mergeFrom (input: CodedInputStream) {
    val builder = DirectionResponse.BuilderDirectionResponse(0)
    mergeWith(builder.parseFrom(input).build())
  }

  //========== Size-related methods ===========
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (code != 0) {
      size += WireFormat.getInt32Size(1, code)
    }
    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
    return size
  }

  fun getSizeNoTag(): Int {
    var size = 0
    if (code != 0) {
      size += WireFormat.getInt32Size(1, code)
    }
    return size
  }

  //========== Builder ===========
  class BuilderDirectionResponse constructor (var code: Int) {
    //========== Properties ===========
    //int32 code = 1
    fun setCode(value: Int): DirectionResponse.BuilderDirectionResponse {
      code = value
      return this
    }

    var errorCode: Int = 0

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
      //int32 code = 1
      if (code != 0) {
        output.writeInt32 (1, code)
      }

    }

    //========== Mutating methods ===========
    fun build(): DirectionResponse {
      val res = DirectionResponse(code)
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
          code = input.readInt32NoTag()
        }
        else -> errorCode = 4
      }
      return true
    }

    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): DirectionResponse.BuilderDirectionResponse {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { errorCode = 2 }
      return this
    }

    fun parseFrom(input: CodedInputStream): DirectionResponse.BuilderDirectionResponse {
      while(parseFieldFrom(input)) {}
      return this
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (code != 0) {
        size += WireFormat.getInt32Size(1, code)
      }
      size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
      return size
    }

    fun getSizeNoTag(): Int {
      var size = 0
      if (code != 0) {
        size += WireFormat.getInt32Size(1, code)
      }
      return size
    }

  }

}


