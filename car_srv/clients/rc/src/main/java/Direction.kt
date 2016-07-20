class DirectionRequest private constructor (command: Command = DirectionRequest.Command.fromIntToCommand(0)) {
  var command : Command
    private set


  init {
    this.command = command
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

  fun writeTo (output: CodedOutputStream): Unit {
    output.writeEnum (1, command.ord)
  }

  class BuilderDirectionRequest constructor (command: Command = DirectionRequest.Command.fromIntToCommand(0)) {
    var command : Command
      private set
    fun setCommand(value: Command): DirectionRequest.BuilderDirectionRequest {
      command = value
      return this
    }


    init {
      this.command = command
    }

    fun readFrom (input: CodedInputStream): DirectionRequest.BuilderDirectionRequest {
      command = Command.fromIntToCommand(input.readEnum(1))
      return this
}

    fun build(): DirectionRequest {
      return DirectionRequest(command)
    }

    fun parseFieldFrom(input: CodedInputStream): Boolean {
      if (input.isAtEnd()) { return false }
      val tag = input.readInt32NoTag()
      if (tag == 0) { return false } 
      val fieldNumber = WireFormat.getTagFieldNumber(tag)
      val wireType = WireFormat.getTagWireType(tag)
      when(fieldNumber) {
        1 -> command = Command.fromIntToCommand(input.readEnumNoTag())
      }
      return true}
    fun parseFrom(input: CodedInputStream): DirectionRequest.BuilderDirectionRequest {
      while(parseFieldFrom(input)) {}
      return this
    }
    fun getSize(): Int {
      var size = 0
      size += WireFormat.getEnumSize(1, command.ord)
      return size
    }
  }


  fun mergeWith (other: DirectionRequest) {
    command = other.command
  }

  fun mergeFrom (input: CodedInputStream) {
    val builder = DirectionRequest.BuilderDirectionRequest()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(): Int {
    var size = 0
    size += WireFormat.getEnumSize(1, command.ord)
    return size
  }
}


class DirectionResponse private constructor (code: Int = 0, errorMsg: kotlin.String = "") {
  var code : Int
    private set

  var errorMsg : kotlin.String
    private set


  init {
    this.code = code
    this.errorMsg = errorMsg
  }

  fun writeTo (output: CodedOutputStream): Unit {
    output.writeInt32 (1, code)
    output.writeString (2, errorMsg)
  }

  class BuilderDirectionResponse constructor (code: Int = 0, errorMsg: kotlin.String = "") {
    var code : Int
      private set
    fun setCode(value: Int): DirectionResponse.BuilderDirectionResponse {
      code = value
      return this
    }

    var errorMsg : kotlin.String
      private set
    fun setErrorMsg(value: kotlin.String): DirectionResponse.BuilderDirectionResponse {
      errorMsg = value
      return this
    }


    init {
      this.code = code
      this.errorMsg = errorMsg
    }

    fun readFrom (input: CodedInputStream): DirectionResponse.BuilderDirectionResponse {
      code = input.readInt32(1)
      errorMsg = input.readString(2)
      return this
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
        1 -> code = input.readInt32NoTag()
        2 -> errorMsg = input.readStringNoTag()
      }
      return true}
    fun parseFrom(input: CodedInputStream): DirectionResponse.BuilderDirectionResponse {
      while(parseFieldFrom(input)) {}
      return this
    }
    fun getSize(): Int {
      var size = 0
      size += WireFormat.getInt32Size(1, code)
      size += WireFormat.getStringSize(2, errorMsg)
      return size
    }
  }


  fun mergeWith (other: DirectionResponse) {
    code = other.code
    errorMsg = other.errorMsg
  }

  fun mergeFrom (input: CodedInputStream) {
    val builder = DirectionResponse.BuilderDirectionResponse()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(): Int {
    var size = 0
    size += WireFormat.getInt32Size(1, code)
    size += WireFormat.getStringSize(2, errorMsg)
    return size
  }
}


