// PROBLEM: none
annotation class Inject

class Test {
    val x = 1
        @Inject get<caret>
}