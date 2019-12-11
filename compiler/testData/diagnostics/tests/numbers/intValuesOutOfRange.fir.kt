// !WITH_NEW_INFERENCE
package a

fun foo(i: Int) = i

fun bar(l: Long) = l

fun main() {
    val i = <!ILLEGAL_CONST_EXPRESSION, INFERENCE_ERROR, INFERENCE_ERROR!>111111111111111777777777777777<!>

    //todo add diagnostic text messages
    //report only 'The value is out of range'
    //not 'An integer literal does not conform to the expected type Int/Long'
    val l: Long = <!ILLEGAL_CONST_EXPRESSION, INFERENCE_ERROR!>1111111111111117777777777777777<!>
    foo(<!ILLEGAL_CONST_EXPRESSION, INFERENCE_ERROR!>11111111111111177777777777777<!>)
    bar(<!ILLEGAL_CONST_EXPRESSION, INFERENCE_ERROR!>11111111111111177777777777777<!>)
}
