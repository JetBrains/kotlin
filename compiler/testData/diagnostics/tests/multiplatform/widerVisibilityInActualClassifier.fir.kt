// ISSUE: KT-59355

// MODULE: common
internal expect open class Some {
    protected class ProtectedNested
    internal class InternalNested

    public fun publicFun()
    internal fun internalFun()
    protected fun protectedFun()
}

internal expect open class Other {
    protected class ProtectedNested
    internal class InternalNested
}

// MODULE: platform-jvm()()(common)
public actual open class Some { // should be allowed
    public class ProtectedNested  // should be allowed
    public class InternalNested // should be allowed

    public actual fun publicFun() {} // should be allowed
    public actual fun internalFun() {} // should be allowed
    public actual fun protectedFun() {} // should be allowed
}

public open class PlatformOther { // should be allowed
    public class ProtectedNested  // should be allowed
    public class InternalNested // should be allowed
}

internal actual typealias Other = PlatformOther // should be allowed

