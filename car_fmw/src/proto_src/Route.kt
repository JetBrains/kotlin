class RouteRequest private constructor (var times: IntArray, var directions: IntArray) {
  //========== Properties ===========
  //repeated int32 times = 1

  //repeated int32 directions = 2

  var errorCode: Int = 0

  //========== Serialization methods ===========
  fun writeTo (output: CodedOutputStream) {
    //repeated int32 times = 1
    if (times.size > 0) {
      output.writeTag(1, WireType.LENGTH_DELIMITED)
      var arrayByteSize = 0

      if (times.size != 0) {
        do {
          var arraySize = 0
          var i = 0
          while (i < times.size) {
            arraySize += WireFormat.getInt32SizeNoTag(times[i])
            i += 1
          } 
          arrayByteSize += arraySize
        } while(false)
      }
      output.writeInt32NoTag(arrayByteSize)

      do {
        var i = 0
        while (i < times.size) {
          output.writeInt32NoTag (times[i])
          i += 1
        }
      } while(false)
    }

    //repeated int32 directions = 2
    if (directions.size > 0) {
      output.writeTag(2, WireType.LENGTH_DELIMITED)
      var arrayByteSize = 0

      if (directions.size != 0) {
        do {
          var arraySize = 0
          var i = 0
          while (i < directions.size) {
            arraySize += WireFormat.getInt32SizeNoTag(directions[i])
            i += 1
          } 
          arrayByteSize += arraySize
        } while(false)
      }
      output.writeInt32NoTag(arrayByteSize)

      do {
        var i = 0
        while (i < directions.size) {
          output.writeInt32NoTag (directions[i])
          i += 1
        }
      } while(false)
    }

  }

  fun mergeWith (other: RouteRequest) {
    times = times.plus((other.times))
    directions = directions.plus((other.directions))
    this.errorCode = other.errorCode
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = RouteRequest.BuilderRouteRequest(IntArray(0), IntArray(0))
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())
  }

  fun mergeFrom (input: CodedInputStream) {
    val builder = RouteRequest.BuilderRouteRequest(IntArray(0), IntArray(0))
    mergeWith(builder.parseFrom(input).build())
  }

  //========== Size-related methods ===========
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (times.size != 0) {
      do {
        var arraySize = 0
        size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
        var i = 0
        while (i < times.size) {
          arraySize += WireFormat.getInt32SizeNoTag(times[i])
          i += 1
        } 
        size += arraySize
        size += WireFormat.getInt32SizeNoTag(arraySize)
      } while(false)
    }
    if (directions.size != 0) {
      do {
        var arraySize = 0
        size += WireFormat.getTagSize(2, WireType.LENGTH_DELIMITED)
        var i = 0
        while (i < directions.size) {
          arraySize += WireFormat.getInt32SizeNoTag(directions[i])
          i += 1
        } 
        size += arraySize
        size += WireFormat.getInt32SizeNoTag(arraySize)
      } while(false)
    }
    size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
    return size
  }

  fun getSizeNoTag(): Int {
    var size = 0
    if (times.size != 0) {
      do {
        var arraySize = 0
        size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
        var i = 0
        while (i < times.size) {
          arraySize += WireFormat.getInt32SizeNoTag(times[i])
          i += 1
        } 
        size += arraySize
        size += WireFormat.getInt32SizeNoTag(arraySize)
      } while(false)
    }
    if (directions.size != 0) {
      do {
        var arraySize = 0
        size += WireFormat.getTagSize(2, WireType.LENGTH_DELIMITED)
        var i = 0
        while (i < directions.size) {
          arraySize += WireFormat.getInt32SizeNoTag(directions[i])
          i += 1
        } 
        size += arraySize
        size += WireFormat.getInt32SizeNoTag(arraySize)
      } while(false)
    }
    return size
  }

  //========== Builder ===========
  class BuilderRouteRequest constructor (var times: IntArray, var directions: IntArray) {
    //========== Properties ===========
    //repeated int32 times = 1
    fun setTimes(value: IntArray): RouteRequest.BuilderRouteRequest {
      times = value
      return this
    }
    fun settimesByIndex(index: Int, value: Int): RouteRequest.BuilderRouteRequest {
      times[index] = value
      return this
    }

    //repeated int32 directions = 2
    fun setDirections(value: IntArray): RouteRequest.BuilderRouteRequest {
      directions = value
      return this
    }
    fun setdirectionsByIndex(index: Int, value: Int): RouteRequest.BuilderRouteRequest {
      directions[index] = value
      return this
    }

    var errorCode: Int = 0

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
      //repeated int32 times = 1
      if (times.size > 0) {
        output.writeTag(1, WireType.LENGTH_DELIMITED)
        var arrayByteSize = 0

        if (times.size != 0) {
          do {
            var arraySize = 0
            var i = 0
            while (i < times.size) {
              arraySize += WireFormat.getInt32SizeNoTag(times[i])
              i += 1
            } 
            arrayByteSize += arraySize
          } while(false)
        }
        output.writeInt32NoTag(arrayByteSize)

        do {
          var i = 0
          while (i < times.size) {
            output.writeInt32NoTag (times[i])
            i += 1
          }
        } while(false)
      }

      //repeated int32 directions = 2
      if (directions.size > 0) {
        output.writeTag(2, WireType.LENGTH_DELIMITED)
        var arrayByteSize = 0

        if (directions.size != 0) {
          do {
            var arraySize = 0
            var i = 0
            while (i < directions.size) {
              arraySize += WireFormat.getInt32SizeNoTag(directions[i])
              i += 1
            } 
            arrayByteSize += arraySize
          } while(false)
        }
        output.writeInt32NoTag(arrayByteSize)

        do {
          var i = 0
          while (i < directions.size) {
            output.writeInt32NoTag (directions[i])
            i += 1
          }
        } while(false)
      }

    }

    //========== Mutating methods ===========
    fun build(): RouteRequest {
      val res = RouteRequest(times, directions)
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
          if (wireType.id != WireType.LENGTH_DELIMITED.id) {
            errorCode = 1
            return false
          }
          val expectedByteSize = input.readInt32NoTag()
          var readSize = 0
          var arraySize = 0
          input.mark()
          do {
            var i = 0
            while(readSize < expectedByteSize) {
              var tmp = 0
              tmp = input.readInt32NoTag()
              arraySize += 1
              readSize += WireFormat.getInt32SizeNoTag(tmp)
            }
          } while (false)
          var newArray = IntArray(arraySize)
          input.reset()
          do {
            var i = 0
            while(i < arraySize) {
              newArray[i] = input.readInt32NoTag()
              i += 1}
            times = newArray
          } while (false)
        }
        2 -> {
          if (wireType.id != WireType.LENGTH_DELIMITED.id) {
            errorCode = 1
            return false
          }
          val expectedByteSize = input.readInt32NoTag()
          var readSize = 0
          var arraySize = 0
          input.mark()
          do {
            var i = 0
            while(readSize < expectedByteSize) {
              var tmp = 0
              tmp = input.readInt32NoTag()
              arraySize += 1
              readSize += WireFormat.getInt32SizeNoTag(tmp)
            }
          } while (false)
          var newArray = IntArray(arraySize)
          input.reset()
          do {
            var i = 0
            while(i < arraySize) {
              newArray[i] = input.readInt32NoTag()
              i += 1}
            directions = newArray
          } while (false)
        }
        else -> errorCode = 4
      }
      return true
    }

    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): RouteRequest.BuilderRouteRequest {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { errorCode = 2 }
      return this
    }

    fun parseFrom(input: CodedInputStream): RouteRequest.BuilderRouteRequest {
      while(parseFieldFrom(input)) {}
      return this
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (times.size != 0) {
        do {
          var arraySize = 0
          size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
          var i = 0
          while (i < times.size) {
            arraySize += WireFormat.getInt32SizeNoTag(times[i])
            i += 1
          } 
          size += arraySize
          size += WireFormat.getInt32SizeNoTag(arraySize)
        } while(false)
      }
      if (directions.size != 0) {
        do {
          var arraySize = 0
          size += WireFormat.getTagSize(2, WireType.LENGTH_DELIMITED)
          var i = 0
          while (i < directions.size) {
            arraySize += WireFormat.getInt32SizeNoTag(directions[i])
            i += 1
          } 
          size += arraySize
          size += WireFormat.getInt32SizeNoTag(arraySize)
        } while(false)
      }
      size += WireFormat.getVarint32Size(size) + WireFormat.getTagSize(fieldNumber, WireType.LENGTH_DELIMITED)
      return size
    }

    fun getSizeNoTag(): Int {
      var size = 0
      if (times.size != 0) {
        do {
          var arraySize = 0
          size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
          var i = 0
          while (i < times.size) {
            arraySize += WireFormat.getInt32SizeNoTag(times[i])
            i += 1
          } 
          size += arraySize
          size += WireFormat.getInt32SizeNoTag(arraySize)
        } while(false)
      }
      if (directions.size != 0) {
        do {
          var arraySize = 0
          size += WireFormat.getTagSize(2, WireType.LENGTH_DELIMITED)
          var i = 0
          while (i < directions.size) {
            arraySize += WireFormat.getInt32SizeNoTag(directions[i])
            i += 1
          } 
          size += arraySize
          size += WireFormat.getInt32SizeNoTag(arraySize)
        } while(false)
      }
      return size
    }

  }

}


class RouteResponse private constructor (var code: Int) {
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

  fun mergeWith (other: RouteResponse) {
    code = other.code
    this.errorCode = other.errorCode
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = RouteResponse.BuilderRouteResponse(0)
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())
  }

  fun mergeFrom (input: CodedInputStream) {
    val builder = RouteResponse.BuilderRouteResponse(0)
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
  class BuilderRouteResponse constructor (var code: Int) {
    //========== Properties ===========
    //int32 code = 1
    fun setCode(value: Int): RouteResponse.BuilderRouteResponse {
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
    fun build(): RouteResponse {
      val res = RouteResponse(code)
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

    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): RouteResponse.BuilderRouteResponse {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { errorCode = 2 }
      return this
    }

    fun parseFrom(input: CodedInputStream): RouteResponse.BuilderRouteResponse {
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


