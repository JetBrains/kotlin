// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

operator fun Int.plus(a: Int?) = this + a!!

public open class PerfectNumberFinder() {
        open public fun isPerfect(number : Int) : Boolean {
            var factors : MutableList<Int?> = ArrayList<Int?>()
            factors?.add(1)
            factors?.add(number)
            for (i in 2..(Math.sqrt((number).toDouble()) - 1).toInt())
                if (((number % i) == 0)) {
                    factors?.add(i)
                    if (((number / i) != i))
                        factors?.add((number / i))

                }

            var sum : Int = 0
            for (i : Int? in factors)
                sum += i
            return ((sum - number) == number)
        }
}

fun box () = if (PerfectNumberFinder().isPerfect(28)) "OK" else "fail"
