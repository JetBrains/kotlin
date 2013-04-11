open class Base
open class SubBase:Base()
open class SubSubBase:SubBase()
open class OtherBase

fun <T: Base> T.extensionSomeBase() = 12
fun <T: SubBase> T.extensionSomeSubBase() = 12
fun <T: SubSubBase> T.extensionSomeSubSubBase() = 12
fun <T: Base> T?.extensionSomeNull() = 12
fun <T: Base?> T.extensionSomeNullParam() = 12
fun <T: OtherBase> T.extensionSomeOtherBase() = 12

fun some() {
    SubBase().extensionSome<caret>
}

// EXIST: extensionSomeBase
// EXIST: extensionSomeSubBase
// EXIST: extensionSomeNull
// EXIST: extensionSomeNullParam
// ABSENT: extensionSomeOtherBase
// ABSENT: extensionSomeSubSubBase
