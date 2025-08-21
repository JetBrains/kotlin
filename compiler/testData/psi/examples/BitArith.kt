// COMPILATION_ERRORS

fun oneBit(index : Int) = 1 shl index

fun setBit(x : Int, index : Int) = x or oneBit(index)
fun unsetBit(x : Int, index : Int) = x and not(oneBit(index))

fun getBit(x : Int, index : Int) = x and oneBit(index) != 0
fun getBit(x : Int, index : Int) = (x shr index) shl 31 != 0

fun countOnes(x : INumber) {
  var result = 0
  while (x != 0) {
    result += x and 1
    x = x ushr 1
  }
  result
}

fun mostSignificantBit(x : INumber) = (x and oneBit(x.bits - 1) != 0) as Int

fun countOnes(x : INumber) = if (x == 0) 0 else mostSignificantBit(x) + countOnes(x shl 1)

fun Int.matchMask(mask : Int) = this and mask == mask

open class INumber : IComparable<This> {
  val bits : Int

  @Operator
  fun plus(other : This) : This

  @Operator
  fun shl(bits : Int) : This
  // ...
}