import abitestutils.abiTest

fun box() = abiTest {
    val companionInstanceError = linkage("Can not get instance of singleton 'Companion': No class found for symbol '/B.Companion'")

    // fun
    expectFailure(companionInstanceError) { createRemovedCompanionFunRef() }
    expectFailure(companionInstanceError) { removedCompanionFunRefName() }
    expectFailure(companionInstanceError) { removedCompanionFunRefInvoke() }

    // val
    expectFailure(companionInstanceError) { createRemovedCompanionValRef() }
    expectFailure(companionInstanceError) { removedCompanionValRefName() }
    expectFailure(companionInstanceError) { removedCompanionValRefInvoke() }
    expectFailure(companionInstanceError) { removedCompanionValRefGet() }

    // var
    expectFailure(companionInstanceError) { createRemovedCompanionVarRef() }
    expectFailure(companionInstanceError) { removedCompanionVarRefName() }
    expectFailure(companionInstanceError) { removedCompanionVarRefInvoke() }
    expectFailure(companionInstanceError) { removedCompanionVarRefGet() }
    expectFailure(companionInstanceError) { removedCompanionVarRefSet() }

    // Top-level properties in lib3: the first read triggers the file initialization, which fails with the IR linkage error;
    // subsequent reads fail because the file is already marked as failed to initialize.
    expectFailure(companionInstanceError) { removedCompanionFunRef }
    expectFailure(custom { it::class.simpleName == "NoClassDefFoundError" && it.message == "Could not initialize file" }) { removedCompanionValRef }
    expectFailure(custom { it::class.simpleName == "NoClassDefFoundError" && it.message == "Could not initialize file" }) { removedCompanionVarRef }
}
