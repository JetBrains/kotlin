// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57228 K2: annotations for interface member properties implemented by delegation are copied

annotation class Ann

interface IFoo {
    @Ann val testVal: String
    @Ann fun testFun()
    @Ann val String.testExtVal: String
    @Ann fun String.testExtFun()
}

class DFoo(d: IFoo) : IFoo by d
