class A<T>
class AIn<in T>
class AOut<out T>
class A2

open class B<T>
open class BIn<in T>
open class BOut<out T>

open class C
open class D

enum class E
interface I

interface T<T>

open class TSub1 : T<String>
open class TSub2 : T<Int>

fun foo(
    string: String, int: Int,
    strings: List<String>, ints: List<Int>,

    aString: A<String>, aInt: A<Int>,
    aOutString: A<out String>, aOutInt: A<out Int>,
    aOutString2: AOut<String>, aOutInt2: AOut<Int>,
    aInString: A<in String>, aInInt: A<in Int>,
    aInString2: AIn<String>, aInInt2: AIn<Int>,

    bString: B<String>, bInt: B<Int>,
    bOutString: B<out String>, bOutInt: B<out Int>,
    bOutString2: BOut<String>, bOutInt2: BOut<Int>,
    bInString: B<in String>, bInInt: B<in Int>,
    bInString2: BIn<String>, bInInt2: BIn<Int>,

    a2: A2,

    e: E,
    i: I,

    ac: A<C>, ad: A<D>,

    tSub1: TSub1,
    tSub2: TSub2,

    aListInt: A<List<Int>>,
    aSetInt: A<Set<Int>>,
    aListString: A<List<String>>,

    mutableListAny: MutableList<Any>,
    listString: List<String>,
) {
    "a" == "b"
    1 == 2
    <!EQUALITY_NOT_APPLICABLE!>"" == 2<!>

    <!EQUALITY_NOT_APPLICABLE!>string == int<!>
    strings == ints

    aString == aInt
    aOutString == aOutInt
    aInString == aInInt
    aOutString == aInInt
    aInString == aOutInt
    aOutString == aInt
    aInString == aInt
    aOutString2 == aOutInt2
    aInString2 == aInInt2
    aOutString2 == aInInt2
    aInString2 == aOutInt2
    aString == a2

    bString == bInt
    bOutString == bOutInt
    bInString == bInInt
    bOutString == bInInt
    bInString == bOutInt
    bOutString == bInt
    bInString == bInt
    bOutString2 == bOutInt2
    bInString2 == bInInt2
    bOutString2 == bInInt2
    bInString2 == bOutInt2

    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>e == i<!>
    <!EQUALITY_NOT_APPLICABLE!>"" == i<!>
    ac == ad

    tSub1 == tSub2

    aString == bString

    aListInt == aSetInt
    aSetInt == aListString
    aListString == aListInt

    aString == aListString
    bString == aListString

    mutableListAny == listString
}
