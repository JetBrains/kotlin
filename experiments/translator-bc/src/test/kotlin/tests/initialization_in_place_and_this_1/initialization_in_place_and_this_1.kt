class initialization_in_place_and_this_1_slave_1(var size: Int)

class initialization_in_place_and_this_1_master(var buffer: initialization_in_place_and_this_1_slave_1) {
    var output = initialization_in_place_and_this_1_slave_2(buffer)
}

class initialization_in_place_and_this_1_slave_2(val buffer: initialization_in_place_and_this_1_slave_1) {
    var pos = 897
    var sz: Int

    init {
        sz = buffer.size
    }
}


fun initialization_in_place_and_this_1(): Int {
    val slave = initialization_in_place_and_this_1_slave_1(100)
    val master = initialization_in_place_and_this_1_master(slave)
    return master.output.sz
}