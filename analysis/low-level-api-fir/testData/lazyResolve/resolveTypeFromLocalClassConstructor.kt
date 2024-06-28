package one.two

class UnusedClass
interface UsedInterface
class UsedClass : UsedInterface

fun reso<caret>lveMe() {
    class Local(val u: UsedClass)
}
