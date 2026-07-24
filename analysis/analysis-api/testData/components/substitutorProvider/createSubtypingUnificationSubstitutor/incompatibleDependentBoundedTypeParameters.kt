// WITH_STDLIB

class MyClass<out Z>
class Something

fun <X, T : MyClass<X>> targets(target1: Pair<T, X>, target2: X) {
    tar<caret_1_right>get1
    targ<caret_2_right>et2
}

fun <Y> candidates(candidate1: Pair<Y, Y>, candidate2: Something) {
    candi<caret_1_left>date1
    candi<caret_2_left>date2
}
