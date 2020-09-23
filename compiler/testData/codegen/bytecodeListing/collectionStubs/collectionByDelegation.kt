class SimplePlatform

open class TargetPlatform(val componentPlatforms: Set<SimplePlatform>) :
    Collection<SimplePlatform> by componentPlatforms