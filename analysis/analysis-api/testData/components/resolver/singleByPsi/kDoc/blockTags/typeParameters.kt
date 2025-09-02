/**
 * [T<caret_1>T]
 * @param [T<caret_2>T]
 */
fun <TT> foo(TT: String) {}

/**
 * [T<caret_3>T] [R<caret_4>R]
 * @param [T<caret_5>T] [R<caret_6>R]
 * @property [T<caret_7>T] [R<caret_8>R]
 */
class A<TT, RR>(TT: String, val RR: Int)