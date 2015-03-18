package c

fun zzz(i: Int, f: (Int) -> Int) { throw Exception("$i $f")}

fun test() {
    fun foo(): Int = 42

    fun bar(i: Int) = i

    bar(foo(<!NAMED_PARAMETER_NOT_FOUND!>xx<!> = zzz(11) { j: Int -> j + 7 }))

    bar(<!NAMED_PARAMETER_NOT_FOUND!>zz<!> = foo(
      <!NAMED_PARAMETER_NOT_FOUND!>xx<!> = zzz(12) { i: Int -> i + i })<!NO_VALUE_FOR_PARAMETER!>)<!>
}