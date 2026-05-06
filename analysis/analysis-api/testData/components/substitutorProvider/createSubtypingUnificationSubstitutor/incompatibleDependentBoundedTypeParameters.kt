// WITH_STDLIB

class MyClass<out Z>
class Something

fun <X, T : MyClass<X>> targets(target1: Pair<T, X>, target2: X) {
    tar<caret_1_target>get1
    targ<caret_2_target>et2
}

fun <Y> candidates(candidate1: Pair<Y, Y>, candidate2: Something) {
    candi<caret_1_base>date1
    candi<caret_2_base>date2
}

