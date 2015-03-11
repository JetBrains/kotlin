
class A {
    default object {
        private var r: Int = 1;

        fun test(): Int {
            r++
            ++r
            return r
        }

        var holder: String = ""

        var r2: Int = 1;
            get() {
                holder += "getR2"
                return $r2
            }

        fun test2() : Int {
            r2++
            ++r2
            return $r2
        }

        var r3: Int = 1;
            set(p: Int) {
                holder += "setR3"
                $r3 = p
            }

        fun test3() : Int {
            r3++
            ++r3
            return $r3
        }

        var r4: Int = 1;
            get() {
                holder += "getR4"
                return $r4
            }
            set(p: Int) {
                holder += "setR4"
                $r4 = p
            }

        fun test4() : Int {
            r4++
            holder += ":"
            ++r4
            return $r4
        }
    }
}

fun box() : String {
    val p = A.test()
    if (p != 3) return "fail 1: $p"

    val p2 = A.test2()
    var holderValue = A.holder
    if (p2 != 3 || holderValue != "getR2getR2getR2") return "fail 2:  $p2 ${holderValue}"

    A.holder = ""
    val p3 = A.test3()
    if (p3 != 3 || A.holder != "setR3setR3") return "fail 3:  $p3 ${A.holder}"

    A.holder = ""
    val p4 = A.test4()
    if (p4 != 3 || A.holder != "getR4setR4:getR4setR4getR4") return "fail 4:  $p4 ${A.holder}"

    return "OK"
}