package test

public inline fun doCallAlwaysBreak(block: (i: Int)-> Int) : Int {
    var res = 0;
    for (i in 1..10) {
        try {
            block(i)
        } finally {
            break;
        }
    }
    return res
}

public val z: Boolean = true

public inline fun doCallAlwaysBreak2(block: (i: Int)-> Int) : Int {
    var res = 0;
    for (i in 1..10) {
        try {
            res = block(i)
        } finally {
            if (z)
                break
        }
    }
    return res
}

//public inline fun doCallAlwaysBreak2(block: (i: Int)-> Int) : Int {
//    var res = 0;
//    for (i in 1..10) {
//        try {
//            res += block(i)
//        } finally {
//            if (z)
//                break
//        }
//    }
//    return res
//}