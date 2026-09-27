// WITH_FIR_TEST_COMPILER_PLUGIN

// FILE: main.kt
package test

import org.jetbrains.kotlin.plugin.sandbox.TestTopLevelPrivateSuspendFun

@TestTopLevelPrivateSuspendFun
fun selected() {}

// FILE: rejected.kt
package test

import org.jetbrains.kotlin.plugin.sandbox.TestTopLevelPrivateSuspendFun

@TestTopLevelPrivateSuspendFun
class Rejected
