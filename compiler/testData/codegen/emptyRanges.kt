fun box(): String {
  if (IntRange(0, 0) != IntRange.EMPTY) {
    return IntRange.EMPTY.toString()
  }
  if (CharRange(0.toChar(), 0) != CharRange.EMPTY) {
    return CharRange.EMPTY.toString()
  }
  if (ByteRange(0, 0) != ByteRange.EMPTY) {
    return ByteRange.EMPTY.toString()
  }
  if (ShortRange(0, 0) != ShortRange.EMPTY) {
    return ShortRange.EMPTY.toString()
  }
  if (FloatRange(0.toFloat(), 0.toFloat()) != FloatRange.EMPTY) {
    return FloatRange.EMPTY.toString()
  }
  if (LongRange(0, 0) != LongRange.EMPTY) {
    return LongRange.EMPTY.toString()
  }
  if (DoubleRange(0.0, 0.0) != DoubleRange.EMPTY) {
    return DoubleRange.EMPTY.toString()
  }
  return "OK"
}
