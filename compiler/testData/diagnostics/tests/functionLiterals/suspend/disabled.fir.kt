// LANGUAGE: -SuspendFunctionsInFunInterfaces, +JvmIrEnabledByDefault

fun interface I {
    suspend fun foo()
}