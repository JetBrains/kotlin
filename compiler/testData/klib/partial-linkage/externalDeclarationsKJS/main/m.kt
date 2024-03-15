import abitestutils.abiTest
import abitestutils.TestBuilder

fun box() = abiTest {
    val rcifaec = RegularClassInheritedFromAbstractExternalClass()
    val rcifeiifoec = RegularClassInheritedFromExternalInterfaceInheritedFromOpenExternalClass()

    expectSuccess("RegularClassInheritedFromAbstractExternalClass.abstractFunction") { rcifaec.abstractFunction() }
    expectSuccess("RegularClassInheritedFromAbstractExternalClass.removedAbstractFunction") { rcifaec.removedAbstractFunction() }
    expectFailure(nonImplementedCallable("function 'addedAbstractFunction'", "class 'RegularClassInheritedFromAbstractExternalClass'")) { rcifaec.addedAbstractFunction() }
    expectSuccess("AbstractExternalClass.function") { rcifaec.function() }
    expectFailure(linkage("Function 'removedFunction' can not be called: No function found for symbol '/RegularClassInheritedFromAbstractExternalClass.removedFunction'")) { rcifaec.callRemovedFunction() }
    expectSuccess("AbstractExternalClass.addedFunction") { rcifaec.addedFunction() }

    expectSuccess("OpenExternalClass.function") { rcifeiifoec.function() }
    expectSuccess("RegularClassInheritedFromExternalInterfaceInheritedFromOpenExternalClass.abstractFunction") { rcifeiifoec.abstractFunction() }
}

private fun TestBuilder.expectRuntimeFailure(errorMessage: String, block: () -> Any) =
    expectFailure(custom { throwable -> throwable !is Exception && throwable.message == errorMessage }) { block() }
