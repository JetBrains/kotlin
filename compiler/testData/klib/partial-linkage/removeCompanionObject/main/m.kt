import abitestutils.abiTest

fun box() = abiTest {
    val companionInstanceError = linkage("Can not get instance of singleton 'Companion': No class found for symbol '/B.Companion'")

    // direct access
    expectFailure(companionInstanceError) { getRemovedCompanion() }
    expectFailure(companionInstanceError) { removedCompanionValCall() }
    expectFailure(companionInstanceError) { removedCompanionVarCall() }
    expectFailure(companionInstanceError) { removedCompanionVarSetCall() }
    expectFailure(companionInstanceError) { removedCompanionFunCall() }

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

    // Top-level properties, each kept in its own file to avoid file initialization errors that might affect test results.
    expectFailure(companionInstanceError) { removedCompanionFunRef }
    expectFailure(companionInstanceError) { removedCompanionValRef }
    expectFailure(companionInstanceError) { removedCompanionVarRef }
}
