fun main() {
    val call = Jaba::isFoo
    println(call.call(Jaba()))
}

//KT-55393