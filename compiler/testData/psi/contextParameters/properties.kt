context(param1: String, another: List<Int>)
val simple: Int get() = 0

context(parameter1: @Anno("1" + "2") String, parameter2: List<@Anno("str") Int>)
val annotated: String
    get() = "str"
