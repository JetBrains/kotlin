class SessionUpResponse private constructor (code: Int = 0, errorMsg: String = "", sid: Int = 0) {
  var code : Int
    private set

  var errorMsg : String
    private set

  var sid : Int
    private set


  init {
    this.code = code
    this.errorMsg = errorMsg
    this.sid = sid
  }

  fun writeTo (output: CodedOutputStream) {
    if (code != 0) {
      output.writeInt32 (1, code)
    }
    if (errorMsg != "") {
      output.writeString (2, errorMsg)
    }
    if (sid != 0) {
      output.writeInt32 (3, sid)
    }
  }

  class BuilderSessionUpResponse constructor (code: Int = 0, errorMsg: String = "", sid: Int = 0) {
    var code : Int
      private set
    fun setCode(value: Int): SessionUpResponse.BuilderSessionUpResponse {
      code = value
      return this
    }

    var errorMsg : String
      private set
    fun setErrorMsg(value: String): SessionUpResponse.BuilderSessionUpResponse {
      errorMsg = value
      return this
    }

    var sid : Int
      private set
    fun setSid(value: Int): SessionUpResponse.BuilderSessionUpResponse {
      sid = value
      return this
    }


    init {
      this.code = code
      this.errorMsg = errorMsg
      this.sid = sid
    }

    fun writeTo (output: CodedOutputStream) {
      if (code != 0) {
        output.writeInt32 (1, code)
      }
      if (errorMsg != "") {
        output.writeString (2, errorMsg)
      }
      if (sid != 0) {
        output.writeInt32 (3, sid)
      }
    }

    fun build(): SessionUpResponse {
      return SessionUpResponse(code, errorMsg, sid)
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
        3 -> {
          if (wireType != WireType.VARINT) {
            throw InvalidProtocolBufferException("Error: Field number 3 has wire type WireType.VARINT but read ${wireType.toString()}")}
          sid = input.readInt32NoTag()
        }
      }
      return true}
    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): SessionUpResponse.BuilderSessionUpResponse {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { throw InvalidProtocolBufferException("Error: expected size of message $expectedSize, but have read at least ${getSizeNoTag()}") }
      return this
    }
    fun parseFrom(input: CodedInputStream): SessionUpResponse.BuilderSessionUpResponse {
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
      if (sid != 0) {
        size += WireFormat.getInt32Size(3, sid)
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
      if (sid != 0) {
        size += WireFormat.getInt32Size(3, sid)
      }
      return size
    }
  }


  fun mergeWith (other: SessionUpResponse) {
    code = other.code
    errorMsg = other.errorMsg
    sid = other.sid
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = SessionUpResponse.BuilderSessionUpResponse()
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())}

  fun mergeFrom (input: CodedInputStream) {
    val builder = SessionUpResponse.BuilderSessionUpResponse()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (code != 0) {
      size += WireFormat.getInt32Size(1, code)
    }
    if (errorMsg != "") {
      size += WireFormat.getStringSize(2, errorMsg)
    }
    if (sid != 0) {
      size += WireFormat.getInt32Size(3, sid)
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
    if (sid != 0) {
      size += WireFormat.getInt32Size(3, sid)
    }
    return size
  }
}


class SessionDownResponse private constructor (code: Int = 0, errorMsg: String = "") {
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

  class BuilderSessionDownResponse constructor (code: Int = 0, errorMsg: String = "") {
    var code : Int
      private set
    fun setCode(value: Int): SessionDownResponse.BuilderSessionDownResponse {
      code = value
      return this
    }

    var errorMsg : String
      private set
    fun setErrorMsg(value: String): SessionDownResponse.BuilderSessionDownResponse {
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

    fun build(): SessionDownResponse {
      return SessionDownResponse(code, errorMsg)
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
    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): SessionDownResponse.BuilderSessionDownResponse {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { throw InvalidProtocolBufferException("Error: expected size of message $expectedSize, but have read at least ${getSizeNoTag()}") }
      return this
    }
    fun parseFrom(input: CodedInputStream): SessionDownResponse.BuilderSessionDownResponse {
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


  fun mergeWith (other: SessionDownResponse) {
    code = other.code
    errorMsg = other.errorMsg
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = SessionDownResponse.BuilderSessionDownResponse()
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())}

  fun mergeFrom (input: CodedInputStream) {
    val builder = SessionDownResponse.BuilderSessionDownResponse()
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


class SessionDownRequest private constructor (sid: Int = 0) {
  var sid : Int
    private set


  init {
    this.sid = sid
  }

  fun writeTo (output: CodedOutputStream) {
    if (sid != 0) {
      output.writeInt32 (1, sid)
    }
  }

  class BuilderSessionDownRequest constructor (sid: Int = 0) {
    var sid : Int
      private set
    fun setSid(value: Int): SessionDownRequest.BuilderSessionDownRequest {
      sid = value
      return this
    }


    init {
      this.sid = sid
    }

    fun writeTo (output: CodedOutputStream) {
      if (sid != 0) {
        output.writeInt32 (1, sid)
      }
    }

    fun build(): SessionDownRequest {
      return SessionDownRequest(sid)
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
          sid = input.readInt32NoTag()
        }
      }
      return true}
    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): SessionDownRequest.BuilderSessionDownRequest {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { throw InvalidProtocolBufferException("Error: expected size of message $expectedSize, but have read at least ${getSizeNoTag()}") }
      return this
    }
    fun parseFrom(input: CodedInputStream): SessionDownRequest.BuilderSessionDownRequest {
      while(parseFieldFrom(input)) {}
      return this
    }
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (sid != 0) {
        size += WireFormat.getInt32Size(1, sid)
      }
      size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
      return size
    }
    fun getSizeNoTag(): Int {
      var size = 0
      if (sid != 0) {
        size += WireFormat.getInt32Size(1, sid)
      }
      return size
    }
  }


  fun mergeWith (other: SessionDownRequest) {
    sid = other.sid
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = SessionDownRequest.BuilderSessionDownRequest()
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())}

  fun mergeFrom (input: CodedInputStream) {
    val builder = SessionDownRequest.BuilderSessionDownRequest()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (sid != 0) {
      size += WireFormat.getInt32Size(1, sid)
    }
    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
    return size
  }
  fun getSizeNoTag(): Int {
    var size = 0
    if (sid != 0) {
      size += WireFormat.getInt32Size(1, sid)
    }
    return size
  }
}


class HeartBeatRequest private constructor (sid: Int = 0) {
  var sid : Int
    private set


  init {
    this.sid = sid
  }

  fun writeTo (output: CodedOutputStream) {
    if (sid != 0) {
      output.writeInt32 (1, sid)
    }
  }

  class BuilderHeartBeatRequest constructor (sid: Int = 0) {
    var sid : Int
      private set
    fun setSid(value: Int): HeartBeatRequest.BuilderHeartBeatRequest {
      sid = value
      return this
    }


    init {
      this.sid = sid
    }

    fun writeTo (output: CodedOutputStream) {
      if (sid != 0) {
        output.writeInt32 (1, sid)
      }
    }

    fun build(): HeartBeatRequest {
      return HeartBeatRequest(sid)
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
          sid = input.readInt32NoTag()
        }
      }
      return true}
    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): HeartBeatRequest.BuilderHeartBeatRequest {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { throw InvalidProtocolBufferException("Error: expected size of message $expectedSize, but have read at least ${getSizeNoTag()}") }
      return this
    }
    fun parseFrom(input: CodedInputStream): HeartBeatRequest.BuilderHeartBeatRequest {
      while(parseFieldFrom(input)) {}
      return this
    }
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (sid != 0) {
        size += WireFormat.getInt32Size(1, sid)
      }
      size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
      return size
    }
    fun getSizeNoTag(): Int {
      var size = 0
      if (sid != 0) {
        size += WireFormat.getInt32Size(1, sid)
      }
      return size
    }
  }


  fun mergeWith (other: HeartBeatRequest) {
    sid = other.sid
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = HeartBeatRequest.BuilderHeartBeatRequest()
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())}

  fun mergeFrom (input: CodedInputStream) {
    val builder = HeartBeatRequest.BuilderHeartBeatRequest()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (sid != 0) {
      size += WireFormat.getInt32Size(1, sid)
    }
    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
    return size
  }
  fun getSizeNoTag(): Int {
    var size = 0
    if (sid != 0) {
      size += WireFormat.getInt32Size(1, sid)
    }
    return size
  }
}


class HeartBeatResponse private constructor (code: Int = 0, errorMsg: String = "") {
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

  class BuilderHeartBeatResponse constructor (code: Int = 0, errorMsg: String = "") {
    var code : Int
      private set
    fun setCode(value: Int): HeartBeatResponse.BuilderHeartBeatResponse {
      code = value
      return this
    }

    var errorMsg : String
      private set
    fun setErrorMsg(value: String): HeartBeatResponse.BuilderHeartBeatResponse {
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

    fun build(): HeartBeatResponse {
      return HeartBeatResponse(code, errorMsg)
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
    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): HeartBeatResponse.BuilderHeartBeatResponse {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { throw InvalidProtocolBufferException("Error: expected size of message $expectedSize, but have read at least ${getSizeNoTag()}") }
      return this
    }
    fun parseFrom(input: CodedInputStream): HeartBeatResponse.BuilderHeartBeatResponse {
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


  fun mergeWith (other: HeartBeatResponse) {
    code = other.code
    errorMsg = other.errorMsg
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = HeartBeatResponse.BuilderHeartBeatResponse()
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())}

  fun mergeFrom (input: CodedInputStream) {
    val builder = HeartBeatResponse.BuilderHeartBeatResponse()
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


