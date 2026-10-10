// WITH_FIR_TEST_COMPILER_PLUGIN
// LANGUAGE: +NestedTypeAliases

package test

import org.jetbrains.kotlin.plugin.sandbox.AllOpen
import org.jetbrains.kotlin.plugin.sandbox.TestTopLevelPrivateSuspendFun

@AllOpen
annotation class MetaMarker

@MetaMarker
@TestTopLevelPrivateSuspendFun
class Parent {
    fun member() {}

    @TestTopLevelPrivateSuspendFun
    fun annotatedMember() {}

    @TestTopLevelPrivateSuspendFun
    class Nested

    @TestTopLevelPrivateSuspendFun
    typealias NestedAlias = String

    @TestTopLevelPrivateSuspendFun
    companion object {
        @TestTopLevelPrivateSuspendFun
        fun companionFunction() {}
    }
}

@TestTopLevelPrivateSuspendFun
fun topLevelFunction() {}
