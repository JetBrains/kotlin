// FIR_IDENTICAL
@Target(AnnotationTarget.TYPEALIAS)
annotation class TestAnn(val x: String)

@TestAnn("TestTypeAlias")
typealias TestTypeAlias = String