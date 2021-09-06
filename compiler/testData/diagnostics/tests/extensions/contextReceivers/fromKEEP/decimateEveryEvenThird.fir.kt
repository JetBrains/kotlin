// WITH_RUNTIME

fun List<Int>.decimateEveryEvenThird() = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>sequence<!> {
    var counter = 1
    for (e in <!ITERATOR_AMBIGUITY!>this<!UNRESOLVED_LABEL!>@List<!><!>) {
        if (e <!NONE_APPLICABLE!>%<!> 2 == 0 && counter % 3 == 0) {
            yield(e)
        }
        counter += 1
    }
}