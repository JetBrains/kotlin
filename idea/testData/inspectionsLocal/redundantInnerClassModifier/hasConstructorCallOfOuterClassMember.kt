// PROBLEM: none
class Test {
    private <caret>inner class Inner {
        val inner2 = Inner2()
    }

    private inner class Inner2
}