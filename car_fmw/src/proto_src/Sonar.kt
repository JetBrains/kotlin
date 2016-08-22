class SonarRequest private constructor (var angles: IntArray) {
  //========== Properties ===========
  //repeated int32 angles = 1

  var errorCode: Int = 0

  //========== Serialization methods ===========
  fun writeTo (output: CodedOutputStream) {
    //repeated int32 angles = 1
    if (angles.size > 0) {
      output.writeTag(1, WireType.LENGTH_DELIMITED)
      var arrayByteSize = 0

      if (angles.size != 0) {
        do {
          var arraySize = 0
          var i = 0
          while (i < angles.size) {
            arraySize += WireFormat.getInt32SizeNoTag(angles[i])
            i += 1
          } 
          arrayByteSize += arraySize
        } while(false)
      }
      output.writeInt32NoTag(arrayByteSize)

      do {
        var i = 0
        while (i < angles.size) {
          output.writeInt32NoTag (angles[i])
          i += 1
        }
      } while(false)
    }

  }

  fun mergeWith (other: SonarRequest) {
    angles = angles.plus((other.angles))
    this.errorCode = other.errorCode
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = SonarRequest.BuilderSonarRequest(IntArray(0))
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())
  }

  fun mergeFrom (input: CodedInputStream) {
    val builder = SonarRequest.BuilderSonarRequest(IntArray(0))
    mergeWith(builder.parseFrom(input).build())
  }

  //========== Size-related methods ===========
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (angles.size != 0) {
      do {
        var arraySize = 0
        size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
        var i = 0
        while (i < angles.size) {
          arraySize += WireFormat.getInt32SizeNoTag(angles[i])
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
    if (angles.size != 0) {
      do {
        var arraySize = 0
        size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
        var i = 0
        while (i < angles.size) {
          arraySize += WireFormat.getInt32SizeNoTag(angles[i])
          i += 1
        } 
        size += arraySize
        size += WireFormat.getInt32SizeNoTag(arraySize)
      } while(false)
    }
    return size
  }

  //========== Builder ===========
  class BuilderSonarRequest constructor (var angles: IntArray) {
    //========== Properties ===========
    //repeated int32 angles = 1
    fun setAngles(value: IntArray): SonarRequest.BuilderSonarRequest {
      angles = value
      return this
    }
    fun setanglesByIndex(index: Int, value: Int): SonarRequest.BuilderSonarRequest {
      angles[index] = value
      return this
    }

    var errorCode: Int = 0

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
      //repeated int32 angles = 1
      if (angles.size > 0) {
        output.writeTag(1, WireType.LENGTH_DELIMITED)
        var arrayByteSize = 0

        if (angles.size != 0) {
          do {
            var arraySize = 0
            var i = 0
            while (i < angles.size) {
              arraySize += WireFormat.getInt32SizeNoTag(angles[i])
              i += 1
            } 
            arrayByteSize += arraySize
          } while(false)
        }
        output.writeInt32NoTag(arrayByteSize)

        do {
          var i = 0
          while (i < angles.size) {
            output.writeInt32NoTag (angles[i])
            i += 1
          }
        } while(false)
      }

    }

    //========== Mutating methods ===========
    fun build(): SonarRequest {
      val res = SonarRequest(angles)
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
            angles = newArray
          } while (false)
        }
        else -> errorCode = 4
      }
      return true
    }

    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): SonarRequest.BuilderSonarRequest {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { errorCode = 2 }
      return this
    }

    fun parseFrom(input: CodedInputStream): SonarRequest.BuilderSonarRequest {
      while(parseFieldFrom(input)) {}
      return this
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (angles.size != 0) {
        do {
          var arraySize = 0
          size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
          var i = 0
          while (i < angles.size) {
            arraySize += WireFormat.getInt32SizeNoTag(angles[i])
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
      if (angles.size != 0) {
        do {
          var arraySize = 0
          size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
          var i = 0
          while (i < angles.size) {
            arraySize += WireFormat.getInt32SizeNoTag(angles[i])
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


class SonarResponse private constructor (var distances: IntArray) {
  //========== Properties ===========
  //repeated int32 distances = 1

  var errorCode: Int = 0

  //========== Serialization methods ===========
  fun writeTo (output: CodedOutputStream) {
    //repeated int32 distances = 1
    if (distances.size > 0) {
      output.writeTag(1, WireType.LENGTH_DELIMITED)
      var arrayByteSize = 0

      if (distances.size != 0) {
        do {
          var arraySize = 0
          var i = 0
          while (i < distances.size) {
            arraySize += WireFormat.getInt32SizeNoTag(distances[i])
            i += 1
          } 
          arrayByteSize += arraySize
        } while(false)
      }
      output.writeInt32NoTag(arrayByteSize)

      do {
        var i = 0
        while (i < distances.size) {
          output.writeInt32NoTag (distances[i])
          i += 1
        }
      } while(false)
    }

  }

  fun mergeWith (other: SonarResponse) {
    distances = distances.plus((other.distances))
    this.errorCode = other.errorCode
  }

  fun mergeFromWithSize (input: CodedInputStream, expectedSize: Int) {
    val builder = SonarResponse.BuilderSonarResponse(IntArray(0))
    mergeWith(builder.parseFromWithSize(input, expectedSize).build())
  }

  fun mergeFrom (input: CodedInputStream) {
    val builder = SonarResponse.BuilderSonarResponse(IntArray(0))
    mergeWith(builder.parseFrom(input).build())
  }

  //========== Size-related methods ===========
  fun getSize(fieldNumber: Int): Int {
    var size = 0
    if (distances.size != 0) {
      do {
        var arraySize = 0
        size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
        var i = 0
        while (i < distances.size) {
          arraySize += WireFormat.getInt32SizeNoTag(distances[i])
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
    if (distances.size != 0) {
      do {
        var arraySize = 0
        size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
        var i = 0
        while (i < distances.size) {
          arraySize += WireFormat.getInt32SizeNoTag(distances[i])
          i += 1
        } 
        size += arraySize
        size += WireFormat.getInt32SizeNoTag(arraySize)
      } while(false)
    }
    return size
  }

  //========== Builder ===========
  class BuilderSonarResponse constructor (var distances: IntArray) {
    //========== Properties ===========
    //repeated int32 distances = 1
    fun setDistances(value: IntArray): SonarResponse.BuilderSonarResponse {
      distances = value
      return this
    }
    fun setdistancesByIndex(index: Int, value: Int): SonarResponse.BuilderSonarResponse {
      distances[index] = value
      return this
    }

    var errorCode: Int = 0

    //========== Serialization methods ===========
    fun writeTo (output: CodedOutputStream) {
      //repeated int32 distances = 1
      if (distances.size > 0) {
        output.writeTag(1, WireType.LENGTH_DELIMITED)
        var arrayByteSize = 0

        if (distances.size != 0) {
          do {
            var arraySize = 0
            var i = 0
            while (i < distances.size) {
              arraySize += WireFormat.getInt32SizeNoTag(distances[i])
              i += 1
            } 
            arrayByteSize += arraySize
          } while(false)
        }
        output.writeInt32NoTag(arrayByteSize)

        do {
          var i = 0
          while (i < distances.size) {
            output.writeInt32NoTag (distances[i])
            i += 1
          }
        } while(false)
      }

    }

    //========== Mutating methods ===========
    fun build(): SonarResponse {
      val res = SonarResponse(distances)
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
            distances = newArray
          } while (false)
        }
        else -> errorCode = 4
      }
      return true
    }

    fun parseFromWithSize(input: CodedInputStream, expectedSize: Int): SonarResponse.BuilderSonarResponse {
      while(getSizeNoTag() < expectedSize) {
        parseFieldFrom(input)
      }
      if (getSizeNoTag() > expectedSize) { errorCode = 2 }
      return this
    }

    fun parseFrom(input: CodedInputStream): SonarResponse.BuilderSonarResponse {
      while(parseFieldFrom(input)) {}
      return this
    }

    //========== Size-related methods ===========
    fun getSize(fieldNumber: Int): Int {
      var size = 0
      if (distances.size != 0) {
        do {
          var arraySize = 0
          size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
          var i = 0
          while (i < distances.size) {
            arraySize += WireFormat.getInt32SizeNoTag(distances[i])
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
      if (distances.size != 0) {
        do {
          var arraySize = 0
          size += WireFormat.getTagSize(1, WireType.LENGTH_DELIMITED)
          var i = 0
          while (i < distances.size) {
            arraySize += WireFormat.getInt32SizeNoTag(distances[i])
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


