fun main(args : Array<String>) {
    val lambdas = ArrayList<() -> Unit>()

    for (i in 0..1) {
        var x = Integer(0)
        val istr = i.toString()

        lambdas.add {
            println(istr)
            println(x.toString())
            x = x + 1
        }
    }

    val lambda1 = lambdas[0]
    val lambda2 = lambdas[1]

    lambda1()
    lambda2()
    lambda1()
    lambda2()
}

class Integer(val value: Int) {
    override fun toString() = value.toString()
    operator fun plus(other: Int) = Integer(value + other)
}