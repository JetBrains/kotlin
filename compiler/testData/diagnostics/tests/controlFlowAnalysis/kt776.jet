//KT-776 Wrong detection of unreachable code

package kt776

fun test5() : Int {
    var x = 0
    while(true) {
        try {
            if(x < 10) {
                x++
                continue
            }
            else {
                break
            }
        }
        finally {
            x++
        }
    }
    return x
}

fun test1() : Int {
    try {
        if (true) {
            return 1
        }
        else {
            return 2
        }
    }
    finally {
        doSmth()  //unreachable
    }
}

fun doSmth() {}
