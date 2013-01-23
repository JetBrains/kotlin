fun box(): String {
  if (IntRange(1, 0) != IntRange.EMPTY) {
    return IntRange.EMPTY.toString()
  }
  if (CharRange(1.toChar(), 0.toChar()) != CharRange.EMPTY) {
    return CharRange.EMPTY.toString()
  }
  if (ByteRange(1, 0) != ByteRange.EMPTY) {
    return ByteRange.EMPTY.toString()
  }
  if (ShortRange(1, 0) != ShortRange.EMPTY) {
    return ShortRange.EMPTY.toString()
  }
  if (FloatRange(1.toFloat(), 0.toFloat()) != FloatRange.EMPTY) {
    return FloatRange.EMPTY.toString()
  }
  if (LongRange(1, 0) != LongRange.EMPTY) {
    return LongRange.EMPTY.toString()
  }
  if (DoubleRange(1.0, 0.0) != DoubleRange.EMPTY) {
    return DoubleRange.EMPTY.toString()
  }
  return "OK"
}
