fun referencedFunctionBefore() {}
fun referencedFunctionAfter() {}

const val kCallableName = ::referencedFunctionBefore.name

enum class TestEnum {
    TopLevelV1,
    TopLevelV2,
    ObjectV1,
    ObjectV2,
    CompanionV1,
    CompanionV2,
    CompanionObjectV1,
    CompanionObjectV2,
}

val toConstTrim = "  top-level-v1  ".trim()
val toConstEnumName = TestEnum.TopLevelV1.name
val toConstUnsignedOperation = 1u.plus(2u)
val toConstCharConstructor = Char(42)

object O {
    val toConstTrimEnd = "object-v1  ".trimEnd()
    val toConstEnumName = TestEnum.ObjectV1.name
    val toConstUnsignedOperation = 2u.times(3u)
    val toConstCharConstructor = Char(42)
}

class C {
    companion {
        val toConstTrimIndent = """    companion-v1""".trimIndent()
        val toConstEnumName = TestEnum.CompanionV1.name
        val toConstUnsignedOperation = 2u.and(3u)
        val toConstCharConstructor = Char(42)
    }

    companion object {
        val companionObjectToConstTrimMargin = """|companion-object-v1""".trimMargin()
        val companionObjectToConstEnumName = TestEnum.CompanionObjectV1.name
        val companionObjectToConstUnsignedOperation = 3u.or(4u)
        val companionObjectToConstCharConstructor = Char(42)
    }
}
