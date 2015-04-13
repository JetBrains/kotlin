package test

val globalProp1: Int = 1
var globalProp2: Int = 2

val String.globalExtensionProp: Int get() = 0
fun String.globalExtensionFun(): Int = 0

object Some {
    var globalProp3: Int = 3
}
