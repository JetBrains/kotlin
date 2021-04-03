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
) {
    "a" == "b"
    1 == 2
    <!EQUALITY_NOT_APPLICABLE!>"" == 2<!>

    <!EQUALITY_NOT_APPLICABLE!>string == int<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>strings == ints<!>

    <!EQUALITY_NOT_APPLICABLE_WARNING!>aString == aInt<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>aOutString == aOutInt<!>
    aInString == aInInt
    <!EQUALITY_NOT_APPLICABLE_WARNING!>aOutString == aInInt<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>aInString == aOutInt<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>aOutString == aInt<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>aInString == aInt<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>aOutString2 == aOutInt2<!>
    aInString2 == aInInt2
    <!EQUALITY_NOT_APPLICABLE_WARNING!>aOutString2 == aInInt2<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>aInString2 == aOutInt2<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>aString == a2<!>

    <!EQUALITY_NOT_APPLICABLE_WARNING!>bString == bInt<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>bOutString == bOutInt<!>
    bInString == bInInt
    <!EQUALITY_NOT_APPLICABLE_WARNING!>bOutString == bInInt<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>bInString == bOutInt<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>bOutString == bInt<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>bInString == bInt<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>bOutString2 == bOutInt2<!>
    bInString2 == bInInt2
    <!EQUALITY_NOT_APPLICABLE_WARNING!>bOutString2 == bInInt2<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>bInString2 == bOutInt2<!>

    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>e == i<!>
    <!EQUALITY_NOT_APPLICABLE!>"" == i<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>ac == ad<!>

    <!EQUALITY_NOT_APPLICABLE_WARNING!>tSub1 == tSub2<!>

    <!EQUALITY_NOT_APPLICABLE_WARNING!>aString == bString<!>

    <!EQUALITY_NOT_APPLICABLE_WARNING!>aListInt == aSetInt<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>aSetInt == aListString<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>aListString == aListInt<!>

    <!EQUALITY_NOT_APPLICABLE_WARNING!>aString == aListString<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>bString == aListString<!>
}
