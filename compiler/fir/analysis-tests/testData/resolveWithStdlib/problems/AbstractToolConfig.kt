// FILE: AbstractToolConfig.kt

abstract class AbstractToolConfig {
    private val platformManager = platformManager()
    private val targetManager = platformManager.targetManager()
    val target = targetManager.target

    protected val platform = platformManager.platform(target)

    val llvmHome = platform.absoluteLlvmHome

    abstract fun platformManager(): PlatformManager
}

// FILE: Platform.kt

class Platform(val configurables: Configurables) : Configurables by configurables

abstract class PlatformManager : HostManager() {
    private val loaders = enabled.map {
        it to loadConfigurables(it)
    }.toMap()

    private val platforms = loaders.map {
        it.key to Platform(it.value)
    }.toMap()

    abstract fun targetManager(userRequest: String? = null): TargetManager
    fun platform(target: KonanTarget) = platforms.getValue(target)

    abstract fun loadConfigurables(target: KonanTarget): Configurables
}

// FILE: HostManager.kt

open class HostManager {
    val enabled: List<KonanTarget>
        get() = emptyList()
}

// FILE: Configurables.kt

interface Configurables {

    val llvmHome get() = hostString("llvmHome")

    val absoluteLlvmHome get() = absolute(llvmHome)

    fun absolute(value: String?): String

    fun hostString(key: String): String?
}

// FILE: KonanTarget.kt

sealed class KonanTarget {
    object ANDROID : KonanTarget()

    object IOS : KonanTarget()
}

// FILE: TargetManager.kt

interface TargetManager {
    val target: KonanTarget
}


