// definite true case is processed incorrectly
/*
fun simpleLess() {
    val a = 1
    if(a < 2) {
        a = 3
    }
    else {
        val b = a
    }
    42
}
*/
// questionable assumptions are used here, this case should be studied more accurately later
fun unknownLess(a: Int, c: Int) {
    var b = 1
    if(b < a) {
        b = a
    }
    if(c < b) {
        b = c
    }
    42
}

// Processed incorrectly, in the end only a = {1;3;7} can be obtained. unavailavle value 5 becomes available before merge
/*
fun complexLess(cond1: Boolean, cond2: Boolean) {
    var a = 1
    if(cond1) {
        a = 3
    }
    else if(cond2) {
        a = 5
    }
    if(a < 4) {
        val b = a
    }
    else {
        a = 7
    }
}
*/