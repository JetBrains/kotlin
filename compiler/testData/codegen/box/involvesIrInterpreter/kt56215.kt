object ObjectWithExtension

fun ObjectWithExtension?.nullableExtensionFun(): String =
    if(this == null)
        "Null"
    else
        "Not null"

fun ObjectWithExtension.extensionFun(): String =
    if(this == null)
        "Null"
    else
        "Not null"

fun box(): String {
    val nullObjectWithExtension: ObjectWithExtension? = null
    if ("${nullObjectWithExtension.nullableExtensionFun()}" != "Null") return "Fail 1"
    if ("${ObjectWithExtension.nullableExtensionFun()}" != "Not null") return "Fail 2"

    if ("${ObjectWithExtension.extensionFun()}" != "Not null") return "Fail 3"
    return "OK"
}
