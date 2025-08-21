// LIBRARY_PLATFORMS: JVM

@JvmInline
value class Some(val value: String)

class RegularClass {
    var classProp: Some = Some("1")
    var classPropImplicit = Some("1")
    var Some.classPropInExtension: Int
        get() = 1
        set(value) {}

    fun classFunInReturn(): Some = Some("1")
    fun classFunInImplicitReturn() = Some("1")
    fun classFunInParameter(s: Some) {}
    fun Some.classFunInExtension() {}

    @JvmOverloads
    fun withJvmOverloads(regularParameter: Int = 0, valueClassParameter: Some = Some("str")) {

    }

    @JvmOverloads
    fun withJvmOverloadsButWithoutDefault(valueClassParameter: Some, regularParameter: Int = 0) {

    }

    @JvmOverloads
    fun Some.withJvmOverloadsAndValueReceiver(regularParameter: Int = 0, valueClassParameter: Some = Some("str")) {

    }

    @JvmOverloads
    fun withJvmOverloadsInDifferentPositions(first: Int = 0, second: Some = Some("1"), third: Int = 2, fourth: Some = Some("3")) {

    }

    @JvmOverloads
    @JvmName("specialName")
    fun withJvmOverloadsAndJvmName(first: Int = 0, second: Some = Some("1"), third: Int = 2, fourth: Some = Some("3")) {

    }

    @JvmOverloads
    constructor(regularParameter: Int = 0, valueClassParameter: Some = Some("str"))
}

class Another {
    @JvmOverloads
    constructor(first: Some = Some("1"), second: Int = 2, third: Some = Some("3"))
}

interface RegularInterface {
    var interfaceProp: Some
    var Some.interfacePropInExtension: Int

    fun interfaceFunInReturn(): Some
    fun interfaceFunInParameter(s: Some)
    fun Some.interfaceFunInExtension()
}
