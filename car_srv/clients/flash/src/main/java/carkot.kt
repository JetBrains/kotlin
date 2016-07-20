class Upload private constructor (data: ByteArray = ByteArray(0), base: kotlin.String = "", method: Method = Upload.Method.BuilderMethod().build()) {
  var data : ByteArray
    private set

  var base : kotlin.String
    private set

  var method : Method
    private set


  init {
    this.data = data
    this.base = base
    this.method = method
  }
  class Method private constructor (type: TYPE = Upload.Method.TYPE.fromIntToTYPE(0), port: kotlin.String = "", device: kotlin.String = "", arguments: MutableList <kotlin.String>  = mutableListOf()) {
    var type : TYPE
      private set

    var port : kotlin.String
      private set

    var device : kotlin.String
      private set

    var arguments : MutableList <kotlin.String> 
      private set


    init {
      this.type = type
      this.port = port
      this.device = device
      this.arguments = arguments
    }
    enum class TYPE(val ord: Int) {
      DFU (0),
      STLINK (1);

      companion object {
        fun fromIntToTYPE (ord: Int): TYPE {
          return when (ord) {
            0 -> TYPE.DFU
            1 -> TYPE.STLINK
            else -> throw InvalidProtocolBufferException("Error: got unexpected int ${ord} while parsing TYPE ");
          }
        }
      }
    }

    fun writeTo (output: CodedOutputStream): Unit {
      output.writeEnum (1, type.ord)
      output.writeString (2, port)
      output.writeString (3, device)
      if (arguments.size > 0) {
        output.writeTag(4, WireType.LENGTH_DELIMITED)
        var arrayByteSize = 0
        run {
          var arraySize = 0
          for (item in arguments) {
            arraySize += WireFormat.getStringSize(4, item)
          }
          arrayByteSize += arraySize
        }
        output.writeInt32NoTag(arrayByteSize)
        for (item in arguments) {
          output.writeString (4, item)
        }
      }
    }

    class BuilderMethod constructor (type: TYPE = Upload.Method.TYPE.fromIntToTYPE(0), port: kotlin.String = "", device: kotlin.String = "", arguments: MutableList <kotlin.String>  = mutableListOf()) {
      var type : TYPE
        private set
      fun setType(value: TYPE): Upload.Method.BuilderMethod {
        type = value
        return this
      }

      var port : kotlin.String
        private set
      fun setPort(value: kotlin.String): Upload.Method.BuilderMethod {
        port = value
        return this
      }

      var device : kotlin.String
        private set
      fun setDevice(value: kotlin.String): Upload.Method.BuilderMethod {
        device = value
        return this
      }

      var arguments : MutableList <kotlin.String> 
        private set
      fun setArguments(value: MutableList <kotlin.String> ): Upload.Method.BuilderMethod {
        arguments = value
        return this
      }
      fun setkotlin.String(index: Int, value: kotlin.String):  {
        arguments[index] = value
        return this
      }
      fun addkotlin.String(value: kotlin.String):  {
        arguments.add(value)
        return this
      }
      fun addAllkotlin.String(value: Iterable<kotlin.String>):  {
        for (item in value) {
          arguments.add(item)
        }
        return this
      }


      init {
        this.type = type
        this.port = port
        this.device = device
        this.arguments = arguments
      }

      fun readFrom (input: CodedInputStream): Upload.Method.BuilderMethod {
        type = TYPE.fromIntToTYPE(input.readEnum(1))
        port = input.readString(2)
        device = input.readString(3)
        val tag = input.readTag(4, WireType.LENGTH_DELIMITED)
        val expectedSize = input.readInt32NoTag()
        var readSize = 0
        while(readSize != expectedSize) {
          val tmp: kotlin.String.Builderkotlin.String = kotlin.String.Builderkotlin.String()
          tmp = input.readString(4)
          readSize += WireFormat.getStringSize(4, tmp)
          arguments.add(tmp.build())
        }
        return this
}

      fun build(): Method {
        return Method(type, port, device, arguments)
      }

      fun parseFieldFrom(input: CodedInputStream): Boolean {
        if (input.isAtEnd()) { return false }
        val tag = input.readInt32NoTag()
        if (tag == 0) { return false } 
        val fieldNumber = WireFormat.getTagFieldNumber(tag)
        val wireType = WireFormat.getTagWireType(tag)
        when(fieldNumber) {
          1 -> type = TYPE.fromIntToTYPE(input.readEnumNoTag())
          2 -> port = input.readStringNoTag()
          3 -> device = input.readStringNoTag()
          4 -> {
            val expectedSize = input.readInt32NoTag()
            var readSize = 0
            while(readSize != expectedSize) {
              val tmp: kotlin.String.Builderkotlin.String = kotlin.String.Builderkotlin.String()
              tmp = input.readString(4)
              readSize += WireFormat.getStringSize(4, tmp)
              arguments.add(tmp.build())
            }
          }
        }
        return true}
      fun parseFrom(input: CodedInputStream): Upload.Method.BuilderMethod {
        while(parseFieldFrom(input)) {}
        return this
      }
      fun getSize(): Int {
        var size = 0
        size += WireFormat.getEnumSize(1, type.ord)
        size += WireFormat.getStringSize(2, port)
        size += WireFormat.getStringSize(3, device)
        run {
          var arraySize = 0
          for (item in arguments) {
            arraySize += WireFormat.getStringSize(4, item)
          }
          size += arraySize + WireFormat.getTagSize(4, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(arraySize)
        }
        return size
      }
    }


    fun mergeWith (other: Method) {
      type = other.type
      port = other.port
      device = other.device
      arguments.addAll(other.arguments)
    }

    fun mergeFrom (input: CodedInputStream) {
      val builder = Upload.Method.BuilderMethod()
      mergeWith(builder.parseFrom(input).build())}
    fun getSize(): Int {
      var size = 0
      size += WireFormat.getEnumSize(1, type.ord)
      size += WireFormat.getStringSize(2, port)
      size += WireFormat.getStringSize(3, device)
      run {
        var arraySize = 0
        for (item in arguments) {
          arraySize += WireFormat.getStringSize(4, item)
        }
        size += arraySize + WireFormat.getTagSize(4, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(arraySize)
      }
      return size
    }
  }


  fun writeTo (output: CodedOutputStream): Unit {
    output.writeBytes (1, data)
    output.writeString (2, base)
    output.writeTag(3, WireType.LENGTH_DELIMITED)
    output.writeInt32NoTag(method.getSize())
    method.writeTo(output)
  }

  class BuilderUpload constructor (data: ByteArray = ByteArray(0), base: kotlin.String = "", method: Method = Upload.Method.BuilderMethod().build()) {
    var data : ByteArray
      private set
    fun setData(value: ByteArray): Upload.BuilderUpload {
      data = value
      return this
    }

    var base : kotlin.String
      private set
    fun setBase(value: kotlin.String): Upload.BuilderUpload {
      base = value
      return this
    }

    var method : Method
      private set
    fun setMethod(value: Method): Upload.BuilderUpload {
      method = value
      return this
    }


    init {
      this.data = data
      this.base = base
      this.method = method
    }

    fun readFrom (input: CodedInputStream): Upload.BuilderUpload {
      data = input.readBytes(1)
      base = input.readString(2)
      input.readTag(3, WireType.LENGTH_DELIMITED)
      val expectedSize = input.readInt32NoTag()
      method.mergeFrom(input)
      if (expectedSize != method.getSize()) { throw InvalidProtocolBufferException ("Expected size ${expectedSize} got ${method.getSize()}") }
      return this
}

    fun build(): Upload {
      return Upload(data, base, method)
    }

    fun parseFieldFrom(input: CodedInputStream): Boolean {
      if (input.isAtEnd()) { return false }
      val tag = input.readInt32NoTag()
      if (tag == 0) { return false } 
      val fieldNumber = WireFormat.getTagFieldNumber(tag)
      val wireType = WireFormat.getTagWireType(tag)
      when(fieldNumber) {
        1 -> data = input.readBytesNoTag()
        2 -> base = input.readStringNoTag()
        3 -> {
          input.readTag(3, WireType.LENGTH_DELIMITED)
          val expectedSize = input.readInt32NoTag()
          method.mergeFrom(input)
          if (expectedSize != method.getSize()) { throw InvalidProtocolBufferException ("Expected size ${expectedSize} got ${method.getSize()}") }
        }
      }
      return true}
    fun parseFrom(input: CodedInputStream): Upload.BuilderUpload {
      while(parseFieldFrom(input)) {}
      return this
    }
    fun getSize(): Int {
      var size = 0
      size += WireFormat.getBytesSize(1, data)
      size += WireFormat.getStringSize(2, base)
      size += method.getSize() + WireFormat.getTagSize(3, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(method.getSize())
      return size
    }
  }


  fun mergeWith (other: Upload) {
    data.plus(other.data)
    base = other.base
    method = other.method
  }

  fun mergeFrom (input: CodedInputStream) {
    val builder = Upload.BuilderUpload()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(): Int {
    var size = 0
    size += WireFormat.getBytesSize(1, data)
    size += WireFormat.getStringSize(2, base)
    size += method.getSize() + WireFormat.getTagSize(3, WireType.LENGTH_DELIMITED) + WireFormat.getVarint32Size(method.getSize())
    return size
  }
}


class UploadResult private constructor (stdOut: kotlin.String = "", stdErr: kotlin.String = "", resultCode: Int = 0) {
  var stdOut : kotlin.String
    private set

  var stdErr : kotlin.String
    private set

  var resultCode : Int
    private set


  init {
    this.stdOut = stdOut
    this.stdErr = stdErr
    this.resultCode = resultCode
  }

  fun writeTo (output: CodedOutputStream): Unit {
    output.writeString (1, stdOut)
    output.writeString (2, stdErr)
    output.writeInt32 (3, resultCode)
  }

  class BuilderUploadResult constructor (stdOut: kotlin.String = "", stdErr: kotlin.String = "", resultCode: Int = 0) {
    var stdOut : kotlin.String
      private set
    fun setStdOut(value: kotlin.String): UploadResult.BuilderUploadResult {
      stdOut = value
      return this
    }

    var stdErr : kotlin.String
      private set
    fun setStdErr(value: kotlin.String): UploadResult.BuilderUploadResult {
      stdErr = value
      return this
    }

    var resultCode : Int
      private set
    fun setResultCode(value: Int): UploadResult.BuilderUploadResult {
      resultCode = value
      return this
    }


    init {
      this.stdOut = stdOut
      this.stdErr = stdErr
      this.resultCode = resultCode
    }

    fun readFrom (input: CodedInputStream): UploadResult.BuilderUploadResult {
      stdOut = input.readString(1)
      stdErr = input.readString(2)
      resultCode = input.readInt32(3)
      return this
}

    fun build(): UploadResult {
      return UploadResult(stdOut, stdErr, resultCode)
    }

    fun parseFieldFrom(input: CodedInputStream): Boolean {
      if (input.isAtEnd()) { return false }
      val tag = input.readInt32NoTag()
      if (tag == 0) { return false } 
      val fieldNumber = WireFormat.getTagFieldNumber(tag)
      val wireType = WireFormat.getTagWireType(tag)
      when(fieldNumber) {
        1 -> stdOut = input.readStringNoTag()
        2 -> stdErr = input.readStringNoTag()
        3 -> resultCode = input.readInt32NoTag()
      }
      return true}
    fun parseFrom(input: CodedInputStream): UploadResult.BuilderUploadResult {
      while(parseFieldFrom(input)) {}
      return this
    }
    fun getSize(): Int {
      var size = 0
      size += WireFormat.getStringSize(1, stdOut)
      size += WireFormat.getStringSize(2, stdErr)
      size += WireFormat.getInt32Size(3, resultCode)
      return size
    }
  }


  fun mergeWith (other: UploadResult) {
    stdOut = other.stdOut
    stdErr = other.stdErr
    resultCode = other.resultCode
  }

  fun mergeFrom (input: CodedInputStream) {
    val builder = UploadResult.BuilderUploadResult()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(): Int {
    var size = 0
    size += WireFormat.getStringSize(1, stdOut)
    size += WireFormat.getStringSize(2, stdErr)
    size += WireFormat.getInt32Size(3, resultCode)
    return size
  }
}


class LogMessage private constructor (source: kotlin.String = "", message: ByteArray = ByteArray(0)) {
  var source : kotlin.String
    private set

  var message : ByteArray
    private set


  init {
    this.source = source
    this.message = message
  }

  fun writeTo (output: CodedOutputStream): Unit {
    output.writeString (1, source)
    output.writeBytes (2, message)
  }

  class BuilderLogMessage constructor (source: kotlin.String = "", message: ByteArray = ByteArray(0)) {
    var source : kotlin.String
      private set
    fun setSource(value: kotlin.String): LogMessage.BuilderLogMessage {
      source = value
      return this
    }

    var message : ByteArray
      private set
    fun setMessage(value: ByteArray): LogMessage.BuilderLogMessage {
      message = value
      return this
    }


    init {
      this.source = source
      this.message = message
    }

    fun readFrom (input: CodedInputStream): LogMessage.BuilderLogMessage {
      source = input.readString(1)
      message = input.readBytes(2)
      return this
}

    fun build(): LogMessage {
      return LogMessage(source, message)
    }

    fun parseFieldFrom(input: CodedInputStream): Boolean {
      if (input.isAtEnd()) { return false }
      val tag = input.readInt32NoTag()
      if (tag == 0) { return false } 
      val fieldNumber = WireFormat.getTagFieldNumber(tag)
      val wireType = WireFormat.getTagWireType(tag)
      when(fieldNumber) {
        1 -> source = input.readStringNoTag()
        2 -> message = input.readBytesNoTag()
      }
      return true}
    fun parseFrom(input: CodedInputStream): LogMessage.BuilderLogMessage {
      while(parseFieldFrom(input)) {}
      return this
    }
    fun getSize(): Int {
      var size = 0
      size += WireFormat.getStringSize(1, source)
      size += WireFormat.getBytesSize(2, message)
      return size
    }
  }


  fun mergeWith (other: LogMessage) {
    source = other.source
    message.plus(other.message)
  }

  fun mergeFrom (input: CodedInputStream) {
    val builder = LogMessage.BuilderLogMessage()
    mergeWith(builder.parseFrom(input).build())}
  fun getSize(): Int {
    var size = 0
    size += WireFormat.getStringSize(1, source)
    size += WireFormat.getBytesSize(2, message)
    return size
  }
}


