package a

fun foo(i: Int) = i

fun bar(l: Long) = l

fun main(args: Array<String>) {
    val <!UNUSED_VARIABLE!>i<!> = <!ERROR_COMPILE_TIME_VALUE!>111111111111111777777777777777<!>

    //todo add diagnostic text messages
    //report only 'The value is out of range'
    //not 'An integer literal does not conform to the expected type Int/Long'
    val <!UNUSED_VARIABLE!>l<!>: Long = <!ERROR_COMPILE_TIME_VALUE!>1111111111111117777777777777777<!>
    foo(<!ERROR_COMPILE_TIME_VALUE!>11111111111111177777777777777<!>)
    bar(<!ERROR_COMPILE_TIME_VALUE!>11111111111111177777777777777<!>)
}
