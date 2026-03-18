annotation class Anno(val i: Int)

@Anno(i = <expr>fun foo() = 1</expr>)
abstract class Check {
    abstract var prop: Int
}