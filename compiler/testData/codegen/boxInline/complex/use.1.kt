import test.*


fun Data.test1(d: Data) : Long  {
    val input2 = Input(this)
    val input = Input(this)
    return input.use<Input, Long>{
        val output = Output(d)
        output.use<Output,Long>{
            input.copyTo(output, 10)
        }
    }
}


fun box(): String {

    val result = Data().test1(Data())
    if (result != 100.toLong()) return "test1: ${result}"

    return "OK"
}
