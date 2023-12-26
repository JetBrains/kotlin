// JVM_ABI_K1_K2_DIFF: KT-63828

annotation class Ann

interface IFoo {
    @Ann val testVal: String
    @Ann fun testFun()
    @Ann val String.testExtVal: String
    @Ann fun String.testExtFun()
}

class DFoo(d: IFoo) : IFoo by d
