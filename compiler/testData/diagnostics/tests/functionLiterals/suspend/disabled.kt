// LANGUAGE: -SuspendFunctionsInFunInterfaces, +JvmIrEnabledByDefault

fun interface I {
    <!FUN_INTERFACE_WITH_SUSPEND_FUNCTION!>suspend<!> fun foo()
}