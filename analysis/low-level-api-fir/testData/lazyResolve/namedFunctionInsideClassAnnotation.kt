annotation class Anno(val i: Int)

@Anno(i = fun foo() = 1)
abstract class Ch<caret>eck {
    abstract var prop: Int
}
