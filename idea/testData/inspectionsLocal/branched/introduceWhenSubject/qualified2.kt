import Platform.JvmPlatform

sealed class Platform {
    object JvmPlatform : Platform()
    class Another(val name: String) : Platform()
}

class ModuleInfo(val platform: Platform)

fun foo(moduleInfo: ModuleInfo) = <caret>when {
    JvmPlatform == moduleInfo.platform -> 1
    else -> 0
}