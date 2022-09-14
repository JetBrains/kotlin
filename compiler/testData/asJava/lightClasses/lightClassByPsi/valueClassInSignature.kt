// CHECK_BY_JAVA_FILE

@JvmInline
value class Some(val value: String)

class RegularClass {
    var classProp: Some = Some("1")
    var Some.classPropInExtension: Int
        get() = 1
        set(value) {}

    fun classFunInReturn(): Some = Some("1")
    fun classFunInParameter(s: Some) {}
    fun Some.classFunInExtension() {}
}

interface RegularInterface {
    var interfaceProp: Some
    var Some.interfacePropInExtension: Int

    fun interfaceFunInReturn(): Some = Some("1")
    fun interfaceFunInParameter(s: Some) {}
    fun Some.interfaceFunInExtension() {}
}
