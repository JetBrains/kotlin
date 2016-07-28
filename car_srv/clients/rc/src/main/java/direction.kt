class DirectionRequest private constructor (command: DirectionRequest.Command = DirectionRequest.Command.fromIntToCommand(0), sid: Int = 0) {
  var command : DirectionRequest.Command
    private set

  var sid : Int
    private set


  init {
    this.command = command
    this.sid = sid
  }
  enum class Command(val ord: Int) {
    stop (0),
    forward (1),
    backward (2),
    left (3),
    right (4);

    companion object {
      fun fromIntToCommand (ord: Int): Command {
        return when (ord) {
          0 -> Command.stop
          1 -> Command.forward
          2 -> Command.backward
          3 -> Command.left
          4 -> Command.right
          else -> throw InvalidProtocolBufferException("Error: got unexpected int ${ord} while parsing Command ");
        }
      }
    }
  }

  fun writeTo (output: CodedOutputStream) {
    if (command != DirectionRequest.Command.fromIntToCommand(0)) {
      output.writeEnum (1, command.ord)
    }
    if (sid != 0) {
      output.writeInt32 (2, sid)
    }
  }

  class BuilderDirectionRequest constructor (command: DirectionRequest.Command = DirectionRequest.Command.fromIntToCommand(0), sid: Int = 0) {
    var command : DirectionRequest.Command
      private set
    fun setCommand(value: DirectionRequest.Command): DirectionRequest.BuilderDirectionRequest {
      command = value
      return this
    }

    var sid : Int
      private set
    fun setSid(value: Int): DirectionRequest.BuilderDirectionRequest {
      sid = value
      return this
    }


    init {
      this.command = command
      this.sid = sid
    }

    fun writeTo (output: CodedOutputStream) {
      if (command != DirectionRequest.Command.fromIntToCommand(0)) {
        output.writeEnum (1, command.ord)
      }
      if (sid != 0) {
        output.writeInt32 (2, sid)
      }
    }

    fun build(): DirectionRequest {
      return DirectionRequest(command, sid)
    }

    fun parseFieldFrom(input: CodedInputStream): Boolean {
      if (input.isAtEnd()) { return false }
      val tag = input.readInt32NoTag()
      if (tag == 0) { return false } 
      val fieldNumber = WireFormat.getTagFieldNumber(tag)
      val wireType = WireFormat.getTagWireType(tag)
      when(fieldNumber) {
        1 -> {
          if (wireType != WireType.VARINT) {
            throw InvalidProtocolBufferException("Error: Field number 1 has wire type WireType.VARINT but read ${wireType.toString()}")}
          command = DirectionRequest.Command.fromIntToCommand(input.readEnumNoTag())
        }
        2 -> {
          if (wireType != WireType.VARINT) {
            throw InvalidProtocolBufferException("Error: Field number 2 has wire type WireType.VARINT but read ${wireType.toString()}")}
          sid = input.readInt32NoTag()
        }
      }
      return true}
    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): DirectionRequest.BuilderDirectionRequest {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { throw InvalidProtocolBufferException("Error: expected size of message $expectedSize, but have read at least ${getSizeNoTag()}") }
      return this
    }
    fun parseFrom(input: CodedInputStream): DirectionRequest.BuilderDirectionRequest {
      while(parseFieldFrom(input)) {}
      return this
    }
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (command != DirectionRequest.Command.fromIntToCommand(0)) {
        size += WireFormat.getEnumSize(1, command.ord)
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
        size += WireFormat.getEnumSize(1, command.ord)
      }
      if (sid != 0) {
        size += WireFormat.getInt32Size(2, sid)
      }
      return size
    }
  }


  fun mergeWith (other: DirectionRequest) {
    command = other.command
    sid = other.sid
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = DirectionRequest.BuilderDirectionRequest()
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())}

  fun mergeFrom (input: CodedInputStream) {
    val builder = DirectionRequest.BuilderDirectionRequest()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (command != DirectionRequest.Command.fromIntToCommand(0)) {
      size += WireFormat.getEnumSize(1, command.ord)
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
      size += WireFormat.getEnumSize(1, command.ord)
    }
    if (sid != 0) {
      size += WireFormat.getInt32Size(2, sid)
    }
    return size
  }
}


class DirectionResponse private constructor (code: Int = 0, errorMsg: String = "") {
  var code : Int
    private set

  var errorMsg : String
    private set


  init {
    this.code = code
    this.errorMsg = errorMsg
  }

  fun writeTo (output: CodedOutputStream) {
    if (code != 0) {
      output.writeInt32 (1, code)
    }
    if (errorMsg != "") {
      output.writeString (2, errorMsg)
    }
  }

  class BuilderDirectionResponse constructor (code: Int = 0, errorMsg: String = "") {
    var code : Int
      private set
    fun setCode(value: Int): DirectionResponse.BuilderDirectionResponse {
      code = value
      return this
    }

    var errorMsg : String
      private set
    fun setErrorMsg(value: String): DirectionResponse.BuilderDirectionResponse {
      errorMsg = value
      return this
    }


    init {
      this.code = code
      this.errorMsg = errorMsg
    }

    fun writeTo (output: CodedOutputStream) {
      if (code != 0) {
        output.writeInt32 (1, code)
      }
      if (errorMsg != "") {
        output.writeString (2, errorMsg)
      }
    }

    fun build(): DirectionResponse {
      return DirectionResponse(code, errorMsg)
    }

    fun parseFieldFrom(input: CodedInputStream): Boolean {
      if (input.isAtEnd()) { return false }
      val tag = input.readInt32NoTag()
      if (tag == 0) { return false } 
      val fieldNumber = WireFormat.getTagFieldNumber(tag)
      val wireType = WireFormat.getTagWireType(tag)
      when(fieldNumber) {
        1 -> {
          if (wireType != WireType.VARINT) {
            throw InvalidProtocolBufferException("Error: Field number 1 has wire type WireType.VARINT but read ${wireType.toString()}")}
          code = input.readInt32NoTag()
        }
        2 -> {
          if (wireType != WireType.LENGTH_DELIMITED) {
            throw InvalidProtocolBufferException("Error: Field number 2 has wire type WireType.LENGTH_DELIMITED but read ${wireType.toString()}")}
          errorMsg = input.readStringNoTag()
        }
      }
      return true}
    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): DirectionResponse.BuilderDirectionResponse {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { throw InvalidProtocolBufferException("Error: expected size of message $expectedSize, but have read at least ${getSizeNoTag()}") }
      return this
    }
    fun parseFrom(input: CodedInputStream): DirectionResponse.BuilderDirectionResponse {
      while(parseFieldFrom(input)) {}
      return this
    }
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (code != 0) {
        size += WireFormat.getInt32Size(1, code)
      }
      if (errorMsg != "") {
        size += WireFormat.getStringSize(2, errorMsg)
      }
      size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
      return size
    }
    fun getSizeNoTag(): Int {
      var size = 0
      if (code != 0) {
        size += WireFormat.getInt32Size(1, code)
      }
      if (errorMsg != "") {
        size += WireFormat.getStringSize(2, errorMsg)
      }
      return size
    }
  }


  fun mergeWith (other: DirectionResponse) {
    code = other.code
    errorMsg = other.errorMsg
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = DirectionResponse.BuilderDirectionResponse()
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())}

  fun mergeFrom (input: CodedInputStream) {
    val builder = DirectionResponse.BuilderDirectionResponse()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (code != 0) {
      size += WireFormat.getInt32Size(1, code)
    }
    if (errorMsg != "") {
      size += WireFormat.getStringSize(2, errorMsg)
    }
    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
    return size
  }
  fun getSizeNoTag(): Int {
    var size = 0
    if (code != 0) {
      size += WireFormat.getInt32Size(1, code)
    }
    if (errorMsg != "") {
      size += WireFormat.getStringSize(2, errorMsg)
    }
    return size
  }
}


