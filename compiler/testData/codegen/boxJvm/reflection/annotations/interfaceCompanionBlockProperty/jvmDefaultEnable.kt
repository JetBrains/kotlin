// JVM_DEFAULT_MODE: ENABLE
// LANGUAGE: +CompanionBlocks
// WITH_REFLECT

import kotlin.test.assertTrue

annotation class Ann

interface A {
    @Ann val member: Int

    companion {
        @Ann val companion = 1
    }
}

fun box(): String {
    assertTrue(A::member.annotations.single() is Ann)
    assertTrue(A::companion.annotations.single() is Ann)
    assertTrue(A::class.members.single { it.name == "member" }.annotations.single() is Ann)
    assertTrue(A::class.members.single { it.name == "companion" }.annotations.single() is Ann)
    return "OK"
}
