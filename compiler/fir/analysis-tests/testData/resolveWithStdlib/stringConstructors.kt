// FULL_JDK

fun consumeString(s: String) {}

fun foo(
    byteArray: ByteArray,
    charArray: CharArray,
    intArray: IntArray,
    charset: java.nio.charset.Charset,
    stringBuilder: java.lang.StringBuilder,
    stringBuffer: java.lang.StringBuffer
) {
    consumeString(String())
    consumeString(String(byteArray, 0, 1, charset))
    consumeString(String(byteArray, charset))
    consumeString(String(byteArray))
    consumeString(String(charArray))
    consumeString(String(charArray, 0, 1))
    consumeString(String(intArray, 0, 1))
    consumeString(String(stringBuffer))
    consumeString(String(stringBuilder))
}