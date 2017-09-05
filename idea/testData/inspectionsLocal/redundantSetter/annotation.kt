// PROBLEM: none
annotation class Inject

class Test {
    var x = 1
        @Inject set<caret>
}