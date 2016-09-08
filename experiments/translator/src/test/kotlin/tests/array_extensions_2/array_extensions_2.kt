fun array_extensions_2_plus_array_slave(): Int {
    var array = IntArray(0)
    array = array.plus(10)
    println(array[0])

    return array[0]
}

fun array_extensions_2_plus_array(): Int{
    return array_extensions_2_plus_array_slave()
}
