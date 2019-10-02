class ClassWithInit {
    val someProp: String
    val anotherProp: Int

    init {
        someProp = "someProp"
        anotherProp = 42
    }
}

fun box(): String {
    val classWithInit = ClassWithInit()
    when {
        classWithInit.anotherProp == 42 && classWithInit.someProp == "someProp" -> return "OK"
        else -> return "FAIL"
    }

}