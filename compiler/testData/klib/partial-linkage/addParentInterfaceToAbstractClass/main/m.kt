// ISSUE: KT-54019
import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess(596) { ClassGetMoreInterfaceChild().faceFun().value }
    expectFailure(linkage("Abstract function 'contract' is not implemented in non-abstract class 'ClassGetMoreInterfaceChild'")) { ClassGetMoreInterfaceChild().contract() }
    expectFailure(linkage("Abstract property accessor 'property.<get-property>' is not implemented in non-abstract class 'ClassGetMoreInterfaceChild'")) { ClassGetMoreInterfaceChild().property }
    expectFailure(linkage("Abstract function 'contract' is not implemented in non-abstract class 'FakeOverrideIntersectionChild'")) { FakeOverrideIntersectionChild().contract() }
    expectFailure(linkage("Abstract function 'contract' is not implemented in non-abstract class 'ClassGetFunInterfaceChild'")) { ClassGetFunInterfaceChild().contract() }
}
