package c

fun zzz(i: Int, f: (Int) -> Int) { throw Exception("$i $f")}

fun test() {
    fun foo(): Int = 42

    fun bar(i: Int) = i

    bar(<!INAPPLICABLE_CANDIDATE!>foo<!>(xx = zzz(11) { j: Int -> j + 7 }))

    <!INAPPLICABLE_CANDIDATE!>bar<!>(zz = <!INAPPLICABLE_CANDIDATE!>foo<!>(
      xx = zzz(12) { i: Int -> i + i }))
}