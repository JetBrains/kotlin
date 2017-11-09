annotation class Concat

open class A

class <caret>Abstraction : A() {
    // INFO: {"checked": "true", "toAbstract": "true"}
    @Concat var extraction = 0
}