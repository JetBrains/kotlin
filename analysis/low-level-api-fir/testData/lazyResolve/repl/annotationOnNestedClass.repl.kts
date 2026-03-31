annotation class Anno(val value: String)

class Outer {
    @Anno("nested")
    class Nes<caret>ted
}
