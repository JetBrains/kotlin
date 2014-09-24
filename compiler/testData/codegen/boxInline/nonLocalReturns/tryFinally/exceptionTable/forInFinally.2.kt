package test

public inline fun doCall(block: (i: Int)-> Int, fblock: (i: Int)-> Unit) : Int {
    var res = 0;
    for (i in 1..10) {
        try {
            res = block(i)
        } finally {
            for (i in 1..10) {
                fblock(i)
            }
        }
    }
    return res
}
