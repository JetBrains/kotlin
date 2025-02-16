context(param1: String, another: List<Int>)
fun simple() {

}

context(parameter1: @Anno("1" + "2") String, parameter2: List<@Anno("str") Int>)
fun annotated() {

}

context(@Ann c: String)
fun annotatedParameter() {}