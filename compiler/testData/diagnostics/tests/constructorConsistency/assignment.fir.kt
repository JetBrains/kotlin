class My {
    val x: String

    constructor() {
        val temp = this
        x = bar(temp)
    }

}

fun bar(arg: My) = arg.x
