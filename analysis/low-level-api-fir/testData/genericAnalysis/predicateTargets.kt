// WITH_FIR_TEST_COMPILER_PLUGIN

package test

import org.jetbrains.kotlin.plugin.sandbox.TestTopLevelPrivateSuspendFun

@TestTopLevelPrivateSuspendFun
class TopClass

@TestTopLevelPrivateSuspendFun
interface TopInterface

@TestTopLevelPrivateSuspendFun
object TopObject

@TestTopLevelPrivateSuspendFun
annotation class TopAnnotation

@TestTopLevelPrivateSuspendFun
class Outer {
    @TestTopLevelPrivateSuspendFun
    class Nested

    @TestTopLevelPrivateSuspendFun
    fun memberFunction() {}

    @TestTopLevelPrivateSuspendFun
    val memberProperty: Int get() = 1

    @get:TestTopLevelPrivateSuspendFun
    val accessorProperty: Int get() = 1
}

@TestTopLevelPrivateSuspendFun
fun topLevelFunction() {
    @TestTopLevelPrivateSuspendFun
    fun localFunction() {}

    @TestTopLevelPrivateSuspendFun
    class LocalClass
}

@TestTopLevelPrivateSuspendFun
val topLevelProperty: Int get() = 1

class ConstructorOnly @TestTopLevelPrivateSuspendFun constructor()

@TestTopLevelPrivateSuspendFun
typealias Alias = TopClass

@TestTopLevelPrivateSuspendFun
enum class TopEnum {
    @TestTopLevelPrivateSuspendFun
    ENTRY
}
