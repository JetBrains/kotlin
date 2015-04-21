JavaAnn class MyClass1
JavaAnn() class MyClass2
JavaAnn("asd") class MyClass3
JavaAnn(*array()) class MyClass4


fun box(): String {
    val value1 = javaClass<MyClass1>().getAnnotation(javaClass<JavaAnn>()).value()
    if (value1.size() != 0) return "fail1: ${value1.size()}"

    val value2 = javaClass<MyClass2>().getAnnotation(javaClass<JavaAnn>()).value()
    if (value2.size() != 0) return "fail2: ${value2.size()}"

    val value3 = javaClass<MyClass3>().getAnnotation(javaClass<JavaAnn>()).value()
    if (value3.size() != 1) return "fail3: ${value3.size()}"
    if (value3[0] != "asd") return "fail4: ${value3[0]}"

    val value4 = javaClass<MyClass4>().getAnnotation(javaClass<JavaAnn>()).value()
    if (value4.size() != 0) return "fail 5: ${value4.size()}"

    return "OK"
}
