fun box(): String {
    val args = MyJavaClass::class.java.getAnnotation(JavaAnn::class.java).args
    val argName1 = args[0].simpleName ?: "fail 1"
    val argName2 = args[1].simpleName ?: "fail 2"
    return argName1 + argName2
}
