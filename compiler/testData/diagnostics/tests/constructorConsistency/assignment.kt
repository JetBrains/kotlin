class My {
    val x: String

    constructor() {
        val temp = <!DEBUG_INFO_LEAKING_THIS!>this<!>
        x = bar(temp)
    }

}

fun bar(arg: My) = arg.x
