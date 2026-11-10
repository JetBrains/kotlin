import abitestutils.abiTest
import kotlinx.cinterop.*

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun box() = abiTest {
    expectSuccess(10) { lib2.acceptUnmodifiedProtocol(null, 10) }
    expectFailure(linkage("Function 'acceptRemovedClass' can not be called: Function uses unlinked class symbol 'lib1/RemovedClass'")) {
        lib2.acceptRemovedClass(null, 11)
    }

    expectSuccess(20) { lib2.acceptClassWithUnmodifiedProtocol(null, 20) }
    expectFailure(linkage("Function 'acceptClassWithRemovedProtocol' can not be called: Function uses unlinked class symbol 'lib1/RemovedProtocolProtocol' (via class 'ClassWithRemovedProtocol')")) {
        lib2.acceptClassWithRemovedProtocol(null, 21)
    }
    expectFailure(linkage("Function 'acceptClassWithRemovedBase' can not be called: Function uses unlinked class symbol 'lib1/RemovedClass' (via class 'ClassWithRemovedBase')")) {
        lib2.acceptClassWithRemovedBase(null, 22)
    }

    expectSuccess(30) { lib2.ClassWithUnmodifiedProtocol.staticMethod(30) }
    expectFailure(linkage("Can not get instance of singleton 'ClassWithRemovedProtocol.Companion': Expression uses unlinked class symbol 'lib1/RemovedProtocolProtocolMeta'")) {
        lib2.ClassWithRemovedProtocol.staticMethod(31)
    }

    expectSuccess { lib2.RemovedClassWrapper(); "OK" }

    // This should fail with abstract-method-not-implemented error: KT-91550.
    expectSuccess(40) { lib2.ClassWithBaseClassToProtocol().method(40) }
}

