fun IntRange.forEach(body : (Int) -> Unit) {
    for(i in this) {
        body(i)
    }
}

fun box() : String {
    var seed = 0

    fun local(x: Int) {
        fun deep() {
            seed += x
        }
        fun deep2(x : Int) {
            seed += x
        }
        fun Int.iter() {
                seed += this
        }

        deep()
        deep2(-x)
        x.iter()
        seed += x
    }

    for(i in 1..5) {
        fun Int.iter() {
            seed += this
        }

        local(i)
        (-i).iter()
    }

    fun local2(y: Int) {
        seed += y
    }

    (1..5).forEach {
            local2(it)
    }


    return if(seed == 30) "OK" else seed.toString()
}
