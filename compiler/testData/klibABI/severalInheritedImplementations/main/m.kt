import abitestutils.*
import lib1.*
import lib2.*

private fun TestBuilder.fe(className: String) = linkage("Function 'foo' in class '${className}' inherits more than one default implementation")
private fun TestBuilder.pe(className: String) = linkage("Property accessor 'bar.<get-bar>' in class '${className}' inherits more than one default implementation")

fun box() = abiTest {
    // For now it's not working with caches, because of incorrect lazy-IR usage.
    // Check KT-54019 for details.
    if (testMode.hasCachesEnabled) {
        expectSuccess("OK") { "OK" }
        return@abiTest
    }
    val instance_I_Default = I_Default()
    val instance_Default_I = Default_I()
    val instance_I_RemovedDefault = I_RemovedDefault()
    val instance_RemovedDefault_I = RemovedDefault_I()
    val instance_I_J_Default = I_J_Default()
    val instance_Default_J_I = Default_J_I()
    val instance_I_J_RemovedDefault = I_J_RemovedDefault()
    val instance_RemovedDefault_J_I = RemovedDefault_J_I()
    val instance_Unrelated_I_Default = Unrelated_I_Default()
    val instance_Unrelated_Default_I = Unrelated_Default_I()
    val instance_Unrelated_I_RemovedDefault = Unrelated_I_RemovedDefault()
    val instance_Unrelated_RemovedDefault_I = Unrelated_RemovedDefault_I()
    val instance_Unrelated_I_J_Default = Unrelated_I_J_Default()
    val instance_Unrelated_Default_J_I = Unrelated_Default_J_I()
    val instance_Unrelated_I_J_RemovedDefault = Unrelated_I_J_RemovedDefault()
    val instance_Unrelated_RemovedDefault_J_I = Unrelated_RemovedDefault_J_I()
    val instance_AbstractUnrelated_I_Default = AbstractUnrelated_I_Default()
    val instance_AbstractUnrelated_Default_I = AbstractUnrelated_Default_I()
    val instance_AbstractUnrelated_I_RemovedDefault = AbstractUnrelated_I_RemovedDefault()
    val instance_AbstractUnrelated_RemovedDefault_I = AbstractUnrelated_RemovedDefault_I()
    val instance_AbstractUnrelated_I_J_Default = AbstractUnrelated_I_J_Default()
    val instance_AbstractUnrelated_Default_J_I = AbstractUnrelated_Default_J_I()
    val instance_AbstractUnrelated_I_J_RemovedDefault = AbstractUnrelated_I_J_RemovedDefault()
    val instance_AbstractUnrelated_RemovedDefault_J_I = AbstractUnrelated_RemovedDefault_J_I()
    val instance_WithFakeOverride_I_Default = WithFakeOverride_I_Default()
    val instance_WithFakeOverride_Default_I = WithFakeOverride_Default_I()
    val instance_WithFakeOverride_I_RemovedDefault = WithFakeOverride_I_RemovedDefault()
    val instance_WithFakeOverride_RemovedDefault_I = WithFakeOverride_RemovedDefault_I()
    val instance_WithFakeOverride_I_J_Default = WithFakeOverride_I_J_Default()
    val instance_WithFakeOverride_Default_J_I = WithFakeOverride_Default_J_I()
    val instance_WithFakeOverride_I_J_RemovedDefault = WithFakeOverride_I_J_RemovedDefault()
    val instance_WithFakeOverride_RemovedDefault_J_I = WithFakeOverride_RemovedDefault_J_I()
    val instance_WithRealOverride_I_Default = WithRealOverride_I_Default()
    val instance_WithRealOverride_Default_I = WithRealOverride_Default_I()
    val instance_WithRealOverride_I_RemovedDefault = WithRealOverride_I_RemovedDefault()
    val instance_WithRealOverride_RemovedDefault_I = WithRealOverride_RemovedDefault_I()
    val instance_WithRealOverride_I_J_Default = WithRealOverride_I_J_Default()
    val instance_WithRealOverride_Default_J_I = WithRealOverride_Default_J_I()
    val instance_WithRealOverride_I_J_RemovedDefault = WithRealOverride_I_J_RemovedDefault()
    val instance_WithRealOverride_RemovedDefault_J_I = WithRealOverride_RemovedDefault_J_I()

    expectFailure(fe("I_Default")) { instance_I_Default.foo() }
    expectFailure(fe("Default_I")) { instance_Default_I.foo() }
    expectSuccess("I v2") { instance_I_RemovedDefault.foo() }
    expectSuccess("I v2") { instance_RemovedDefault_I.foo() }
    expectFailure(fe("I_J_Default")) { instance_I_J_Default.foo() }
    expectFailure(fe("Default_J_I")) { instance_Default_J_I.foo() }
    expectFailure(fe("I_J_RemovedDefault")) { instance_I_J_RemovedDefault.foo() }
    expectFailure(fe("RemovedDefault_J_I")) { instance_RemovedDefault_J_I.foo() }
    expectSuccess("Unrelated v2") { instance_Unrelated_I_Default.foo() }
    expectSuccess("Unrelated v2") { instance_Unrelated_Default_I.foo() }
    expectSuccess("Unrelated v2") { instance_Unrelated_I_RemovedDefault.foo() }
    expectSuccess("Unrelated v2") { instance_Unrelated_RemovedDefault_I.foo() }
    expectSuccess("Unrelated v2") { instance_Unrelated_I_J_Default.foo() }
    expectSuccess("Unrelated v2") { instance_Unrelated_Default_J_I.foo() }
    expectSuccess("Unrelated v2") { instance_Unrelated_I_J_RemovedDefault.foo() }
    expectSuccess("Unrelated v2") { instance_Unrelated_RemovedDefault_J_I.foo() }
    expectFailure(fe("AbstractUnrelated_I_Default")) { instance_AbstractUnrelated_I_Default.foo() }
    expectFailure(fe("AbstractUnrelated_Default_I")) { instance_AbstractUnrelated_Default_I.foo() }
    expectSuccess("I v2") { instance_AbstractUnrelated_I_RemovedDefault.foo() }
    expectSuccess("I v2") { instance_AbstractUnrelated_RemovedDefault_I.foo() }
    expectFailure(fe("AbstractUnrelated_I_J_Default")) { instance_AbstractUnrelated_I_J_Default.foo() }
    expectFailure(fe("AbstractUnrelated_Default_J_I")) { instance_AbstractUnrelated_Default_J_I.foo() }
    expectFailure(fe("AbstractUnrelated_I_J_RemovedDefault")) { instance_AbstractUnrelated_I_J_RemovedDefault.foo() }
    expectFailure(fe("AbstractUnrelated_RemovedDefault_J_I")) { instance_AbstractUnrelated_RemovedDefault_J_I.foo() }
    expectFailure(fe("WithFakeOverride_I_Default")) { instance_WithFakeOverride_I_Default.foo() }
    expectFailure(fe("WithFakeOverride_Default_I")) { instance_WithFakeOverride_Default_I.foo() }
    expectSuccess("I v2") { instance_WithFakeOverride_I_RemovedDefault.foo() }
    expectSuccess("I v2") { instance_WithFakeOverride_RemovedDefault_I.foo() }
    expectFailure(fe("WithFakeOverride_I_J_Default")) { instance_WithFakeOverride_I_J_Default.foo() }
    expectFailure(fe("WithFakeOverride_Default_J_I")) { instance_WithFakeOverride_Default_J_I.foo() }
    expectFailure(fe("WithFakeOverride_I_J_RemovedDefault")) { instance_WithFakeOverride_I_J_RemovedDefault.foo() }
    expectFailure(fe("WithFakeOverride_RemovedDefault_J_I")) { instance_WithFakeOverride_RemovedDefault_J_I.foo() }
    expectSuccess("WithRealOverride v2") { instance_WithRealOverride_I_Default.foo() }
    expectSuccess("WithRealOverride v2") { instance_WithRealOverride_Default_I.foo() }
    expectSuccess("WithRealOverride v2") { instance_WithRealOverride_I_RemovedDefault.foo() }
    expectSuccess("WithRealOverride v2") { instance_WithRealOverride_RemovedDefault_I.foo() }
    expectSuccess("WithRealOverride v2") { instance_WithRealOverride_I_J_Default.foo() }
    expectSuccess("WithRealOverride v2") { instance_WithRealOverride_Default_J_I.foo() }
    expectSuccess("WithRealOverride v2") { instance_WithRealOverride_I_J_RemovedDefault.foo() }
    expectSuccess("WithRealOverride v2") { instance_WithRealOverride_RemovedDefault_J_I.foo() }

    expectFailure(pe("I_Default")) { instance_I_Default.bar }
    expectFailure(pe("Default_I")) { instance_Default_I.bar }
    expectSuccess("I v2") { instance_I_RemovedDefault.bar }
    expectSuccess("I v2") { instance_RemovedDefault_I.bar }
    expectFailure(pe("I_J_Default")) { instance_I_J_Default.bar }
    expectFailure(pe("Default_J_I")) { instance_Default_J_I.bar }
    expectFailure(pe("I_J_RemovedDefault")) { instance_I_J_RemovedDefault.bar }
    expectFailure(pe("RemovedDefault_J_I")) { instance_RemovedDefault_J_I.bar }
    expectSuccess("Unrelated v2") { instance_Unrelated_I_Default.bar }
    expectSuccess("Unrelated v2") { instance_Unrelated_Default_I.bar }
    expectSuccess("Unrelated v2") { instance_Unrelated_I_RemovedDefault.bar }
    expectSuccess("Unrelated v2") { instance_Unrelated_RemovedDefault_I.bar }
    expectSuccess("Unrelated v2") { instance_Unrelated_I_J_Default.bar }
    expectSuccess("Unrelated v2") { instance_Unrelated_Default_J_I.bar }
    expectSuccess("Unrelated v2") { instance_Unrelated_I_J_RemovedDefault.bar }
    expectSuccess("Unrelated v2") { instance_Unrelated_RemovedDefault_J_I.bar }
    expectFailure(pe("AbstractUnrelated_I_Default")) { instance_AbstractUnrelated_I_Default.bar }
    expectFailure(pe("AbstractUnrelated_Default_I")) { instance_AbstractUnrelated_Default_I.bar }
    expectSuccess("I v2") { instance_AbstractUnrelated_I_RemovedDefault.bar }
    expectSuccess("I v2") { instance_AbstractUnrelated_RemovedDefault_I.bar }
    expectFailure(pe("AbstractUnrelated_I_J_Default")) { instance_AbstractUnrelated_I_J_Default.bar }
    expectFailure(pe("AbstractUnrelated_Default_J_I")) { instance_AbstractUnrelated_Default_J_I.bar }
    expectFailure(pe("AbstractUnrelated_I_J_RemovedDefault")) { instance_AbstractUnrelated_I_J_RemovedDefault.bar }
    expectFailure(pe("AbstractUnrelated_RemovedDefault_J_I")) { instance_AbstractUnrelated_RemovedDefault_J_I.bar }
    expectFailure(pe("WithFakeOverride_I_Default")) { instance_WithFakeOverride_I_Default.bar }
    expectFailure(pe("WithFakeOverride_Default_I")) { instance_WithFakeOverride_Default_I.bar }
    expectSuccess("I v2") { instance_WithFakeOverride_I_RemovedDefault.bar }
    expectSuccess("I v2") { instance_WithFakeOverride_RemovedDefault_I.bar }
    expectFailure(pe("WithFakeOverride_I_J_Default")) { instance_WithFakeOverride_I_J_Default.bar }
    expectFailure(pe("WithFakeOverride_Default_J_I")) { instance_WithFakeOverride_Default_J_I.bar }
    expectFailure(pe("WithFakeOverride_I_J_RemovedDefault")) { instance_WithFakeOverride_I_J_RemovedDefault.bar }
    expectFailure(pe("WithFakeOverride_RemovedDefault_J_I")) { instance_WithFakeOverride_RemovedDefault_J_I.bar }
    expectSuccess("WithRealOverride v2") { instance_WithRealOverride_I_Default.bar }
    expectSuccess("WithRealOverride v2") { instance_WithRealOverride_Default_I.bar }
    expectSuccess("WithRealOverride v2") { instance_WithRealOverride_I_RemovedDefault.bar }
    expectSuccess("WithRealOverride v2") { instance_WithRealOverride_RemovedDefault_I.bar }
    expectSuccess("WithRealOverride v2") { instance_WithRealOverride_I_J_Default.bar }
    expectSuccess("WithRealOverride v2") { instance_WithRealOverride_Default_J_I.bar }
    expectSuccess("WithRealOverride v2") { instance_WithRealOverride_I_J_RemovedDefault.bar }
    expectSuccess("WithRealOverride v2") { instance_WithRealOverride_RemovedDefault_J_I.bar }
}