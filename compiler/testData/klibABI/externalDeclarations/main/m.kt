import abitestutils.abiTest
import abitestutils.TestBuilder

fun box() = abiTest {
    val ecifaec = ExternalClassInheritedFromAbstractExternalClass()
    val rcifaec = RegularClassInheritedFromAbstractExternalClass()
    val ecifeiifoec = ExternalClassInheritedFromExternalInterfaceInheritedFromOpenExternalClass()
    val rcifeiifoec = RegularClassInheritedFromExternalInterfaceInheritedFromOpenExternalClass()

    expectSuccess("ExternalClassInheritedFromAbstractExternalClass.abstractFunction") { ecifaec.abstractFunction() }
    expectSuccess("ExternalClassInheritedFromAbstractExternalClass.removedAbstractFunction") { ecifaec.removedAbstractFunction() }
    expectRuntimeFailure("Calling abstract function AbstractExternalClass.addedAbstractFunction") { ecifaec.addedAbstractFunction() }
    expectSuccess("AbstractExternalClass.function") { ecifaec.function() }
    expectFailure(linkage("Function 'removedFunction' can not be called: No function found for symbol '/ExternalClassInheritedFromAbstractExternalClass.removedFunction'")) { ecifaec.callRemovedFunction() }
    expectSuccess("AbstractExternalClass.addedFunction") { ecifaec.addedFunction() }

    expectSuccess("RegularClassInheritedFromAbstractExternalClass.abstractFunction") { rcifaec.abstractFunction() }
    expectSuccess("RegularClassInheritedFromAbstractExternalClass.removedAbstractFunction") { rcifaec.removedAbstractFunction() }
    expectFailure(nonImplementedCallable("function 'addedAbstractFunction'", "class 'RegularClassInheritedFromAbstractExternalClass'")) { rcifaec.addedAbstractFunction() }
    expectSuccess("AbstractExternalClass.function") { rcifaec.function() }
    expectFailure(linkage("Function 'removedFunction' can not be called: No function found for symbol '/RegularClassInheritedFromAbstractExternalClass.removedFunction'")) { rcifaec.callRemovedFunction() }
    expectSuccess("AbstractExternalClass.addedFunction") { rcifaec.addedFunction() }

    expectSuccess("OpenExternalClass.function") { ecifeiifoec.function() }
    expectSuccess("ExternalClassInheritedFromExternalInterfaceInheritedFromOpenExternalClass.abstractFunction") { ecifeiifoec.abstractFunction() }
    expectSuccess("OpenExternalClass.function") { rcifeiifoec.function() }
    expectSuccess("RegularClassInheritedFromExternalInterfaceInheritedFromOpenExternalClass.abstractFunction") { rcifeiifoec.abstractFunction() }
}

private inline fun TestBuilder.expectRuntimeFailure(errorMessage: String, noinline block: () -> Any) =
    expectFailure(custom { throwable -> throwable !is Exception && throwable.message == errorMessage }) { block() }
