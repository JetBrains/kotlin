fun test1() : Boolean {
    try {
        return true
    } finally {
          if(true) // otherwise we wisely have unreachable code
            return false
    }
}

var x = true
fun test2() : Boolean {
    try {
    } finally {
        x = false;
    }
    return x
}

fun test3() : Int {
    var y = 0
    try {
        ++y
    } finally {
        ++y
    }
    return y
}

var z = 0
fun test4() : Int {
    z = 0
    return try {
        try {
            z++
        }
        finally {
            z++
        }
    } finally {
        ++z
    }
}

fun test5() : Int {
    var x = 0
    while(true) {
        try {
            if(x < 10)
                x++
            else
                break
        }
        finally {
            x++
        }
    }
    return x
}

fun test6() : Int {
    var x = 0
    while(x < 10) {
        try {
            x++
            continue
        }
        finally {
            x++
        }
    }
    return x
}

fun box() : String {
    if(test1()) return "test1 failed"
    if(test2()) return "test2 failed"
    if(test3() != 2) return "test3 failed"
    if(test4() != 0) return "test4 failed"
    if(test5() != 11) return "test5 failed"
    if(test6() != 10) return "test6 failed"

    return "OK"
}
