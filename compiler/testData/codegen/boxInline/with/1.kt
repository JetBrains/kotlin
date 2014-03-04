import test.*

fun Data.test1(d: Data) : Long  {
    val input = Input(this)
    var result = 10.toLong()
    with(input) {
         result = use<Long>{
            val output = Output(d)
             useNoInline<Long>{
                data()
                copyTo(output, 10)
            }
        }
    }
    return result
}


fun box(): String {

    val result = Data().test1(Data())
    if (result != 100.toLong()) return "test1: ${result}"

    return "OK"
}
