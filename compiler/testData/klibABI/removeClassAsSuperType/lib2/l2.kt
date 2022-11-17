class TopLevelClassChildOfRemovedClass : RemovedClass()
object TopLevelObjectChildOfRemovedClass : RemovedClass()
interface TopLevelInterfaceChildOfRemovedInterface : RemovedInterface
class TopLevelClassChildOfRemovedInterface : RemovedInterface
object TopLevelObjectChildOfRemovedInterface : RemovedInterface
enum class TopLevelEnumClassChildOfRemovedInterface : RemovedInterface { ENTRY }

class TopLevel {
    class NestedClassChildOfRemovedClass : RemovedClass()
    object NestedObjectChildOfRemovedClass : RemovedClass()
    interface NestedInterfaceChildOfRemovedInterface : RemovedInterface
    class NestedClassChildOfRemovedInterface : RemovedInterface
    object NestedObjectChildOfRemovedInterface : RemovedInterface
    enum class NestedEnumClassChildOfRemovedInterface : RemovedInterface { ENTRY }

    inner class InnerClassChildOfRemovedClass : RemovedClass()
    inner class InnerClassChildOfRemovedInterface : RemovedInterface
}

class TopLevelWithCompanionChildOfRemovedClass {
    companion object : RemovedClass()
}

class TopLevelWithCompanionChildOfRemovedInterface {
    companion object : RemovedInterface
}

val anonymousObjectChildOfRemovedClass = object : RemovedClass() {}
val anonymousObjectChildOfRemovedInterface = object : RemovedInterface {}

fun topLevelFunctionWithLocalClassChildOfRemovedClass() {
    class LocalClass : RemovedClass()
    LocalClass().toString()
}

fun topLevelFunctionWithLocalClassChildOfRemovedInterface() {
    class LocalClass : RemovedInterface
    LocalClass().toString()
}

fun topLevelFunctionWithAnonymousObjectChildOfRemovedClass() {
    val anonymousObject = object : RemovedClass() {}
    anonymousObject.toString()
}

fun topLevelFunctionWithAnonymousObjectChildOfRemovedInterface() {
    val anonymousObject = object : RemovedInterface {}
    anonymousObject.toString()
}
