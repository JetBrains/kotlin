object X1 {
    var x1 = 0
    object X2 {
        var x2 = 0
        object X3 {
            var x3 = 0
        }
    }
}

fun test1(a: IntArray) {
    var i = 0
    a[i++]++
}

fun test2() {
    X1.x1++
    X1.X2.x2++
    X1.X2.X3.x3++
}

class B(var s: Int = 0)

object Host {
    operator fun B.plusAssign(b: B) {
        this.s += b.s
    }
}

fun Host.test3(v: B) {
    v += B(1000)
}

