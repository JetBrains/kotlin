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

    // This call succeeds, because while `method` should not have an implementation from the interop Klib point of view,
    // it is still implemented in the binary. Note that if it was also removed from the binary, it would fail to link at runtime,
    // but with an Objective-C exception which we are not able to catch here. See KT-91550 for detauls.
    expectSuccess(40) { lib2.ClassWithBaseClassToProtocol().method(40) }
}

