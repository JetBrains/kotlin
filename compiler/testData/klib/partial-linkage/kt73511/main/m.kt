import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("MyAnnotationHolder(x=42)") { createMyAnnotationHolderInstance(42).toString() }
}
