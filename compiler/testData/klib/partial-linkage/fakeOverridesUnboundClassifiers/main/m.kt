import abitestutils.abiTest

// The class is never used; however, just determining the FOs for it did cause an error described in KT-75766
abstract class IU : I, U

class App : C()

fun box() = abiTest {
  expectFailure(linkage("Function 'doB' can not be called: Function uses unlinked class symbol '/B'")) { App().doB() }
}
