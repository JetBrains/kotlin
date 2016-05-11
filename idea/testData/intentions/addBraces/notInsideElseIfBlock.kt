// IS_APPLICABLE: false
fun foo(p: Int) {
    if (p == 1) {

    }
    else if (p == 2) {
        bar()<caret>
    }
}

fun bar(){}
