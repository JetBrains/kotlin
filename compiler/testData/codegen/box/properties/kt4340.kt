// IGNORE_BACKEND_FIR: JVM_IR
class A {

    var result: Int = 0;

    private val Int.times3: Int
        get() = this * 3

    private var Int.times: Int
        get() = this * 4
        set(s: Int) {
            result = this * s
        }

    fun test(p: Int):Int {
        return {
            p.times3
        }()
    }

    fun test2(p: Int, s: Int):Int {
        {
            p.times = s
        }()
        return result
    }
}

fun box() : String {
    var result = A().test(3);
    if (result != 9) return "fail1: $result"

    result = A().test2(2, 4);
    if (result != 8) return "fail2: $result"

    return "OK"
}