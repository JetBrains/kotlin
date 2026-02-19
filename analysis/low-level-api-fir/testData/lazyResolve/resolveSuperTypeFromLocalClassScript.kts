package one.two

class UnusedClass
interface UsedInterface2
interface UsedInterface : UsedInterface2

fun reso<caret>lveMe() {
    class Local : UsedInterface
}
