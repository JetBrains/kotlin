import test.*

fun specifyOptionalArgument() = typeWithMessage<List<Int>>("Hello")

fun useDefault() = typeWithMessage<List<Int>>()

fun box(): String {
    val specifyOptionalArgument = specifyOptionalArgument()
    val useDefault = useDefault()

    if (useDefault != specifyOptionalArgument) return "fail: $useDefault != $specifyOptionalArgument"

    val type = typeWithMessage<List<Int>>("")
    if (type != " test.TypeRef<java.util.List<? extends java.lang.Integer>>") return "fail 2: $type"

    return "OK"
}