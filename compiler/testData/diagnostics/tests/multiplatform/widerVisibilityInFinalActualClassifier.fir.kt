// ISSUE: KT-59355

// MODULE: common
internal expect class Some {
    internal class InternalNested

    public fun publicFun()
    internal fun internalFun()
}

internal expect class Other {
    internal class InternalNested
}

// MODULE: platform-jvm()()(common)
public actual class Some { // should be allowed
    public class InternalNested // should be allowed

    public actual fun publicFun() {} // should be allowed
    public actual fun internalFun() {} // should be allowed
}

public class PlatformOther { // should be allowed
    public class InternalNested // should be allowed
}

internal actual typealias Other = PlatformOther // should be allowed

