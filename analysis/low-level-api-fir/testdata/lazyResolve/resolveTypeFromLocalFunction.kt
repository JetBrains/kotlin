package one.two

class UnusedClass
interface UsedInterface
class UsedClass : UsedInterface

fun reso<caret>lveMe() {
    class Local
    fun localFunction(usedClass: UsedClass, local: Local) {

    }
}
