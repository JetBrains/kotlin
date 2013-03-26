class Test {

    val property:Int
    ;{
        fun local():Int {
            return 10;
        }
        property = local();
    }

}

fun box(): String {
    return if (Test().property == 10) "OK" else "fail"
}
