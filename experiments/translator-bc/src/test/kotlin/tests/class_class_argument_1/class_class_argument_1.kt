class class_class_argument_1_slave

class class_class_argument_1_master(val buffer: class_class_argument_1_slave) {
    var pos = 99999
}


fun class_class_argument_1(): Int {
    val buffer = class_class_argument_1_slave()
    val output = class_class_argument_1_master(buffer)
    return output.pos
}