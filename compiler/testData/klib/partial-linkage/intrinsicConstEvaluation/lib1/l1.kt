import kotlin.experimental.xor

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
val toConstUnsigned = 1u.plus(2u)
val toConstCharConstructor = Char(42)
const val toValSigned = 1.toByte() xor 2.toByte()

object O {
    val toConstTrimEnd = "object-v1  ".trimEnd()
    val toConstEnumName = TestEnum.ObjectV1.name
    val toConstUnsigned = 2u.times(3u)
    val toConstCharConstructor = Char(42)
    const val toValSigned = 1.toShort().inc()
}

class C {
    companion {
        val toConstTrimIndent = """    companion-v1""".trimIndent()
        val toConstEnumName = TestEnum.CompanionV1.name
        val toConstUnsigned = 2u.and(3u)
        val toConstCharConstructor = Char(42)
        const val toValSigned = 1.dec()
    }

    companion object {
        val companionObjectToConstTrimMargin = """|companion-object-v1""".trimMargin()
        val companionObjectToConstEnumName = TestEnum.CompanionObjectV1.name
        val companionObjectToConstUnsigned = 3u.or(4u)
        val companionObjectToConstCharConstructor = Char(42)
        const val companionObjectToValSigned = 1L.inc()
    }
}
