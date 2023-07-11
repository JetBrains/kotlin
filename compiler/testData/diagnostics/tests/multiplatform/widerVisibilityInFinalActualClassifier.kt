// ISSUE: KT-59355

// MODULE: common
internal expect class <!NO_ACTUAL_FOR_EXPECT!>Some<!> {
    internal class InternalNested

    public fun publicFun()
    internal fun internalFun()
}

internal expect class <!NO_ACTUAL_FOR_EXPECT!>Other<!> {
    internal class InternalNested
}

// MODULE: platform-jvm()()(common)
<!ACTUAL_WITHOUT_EXPECT!>public<!> actual class Some { // should be allowed
    <!ACTUAL_WITHOUT_EXPECT!>public<!> class <!ACTUAL_MISSING!>InternalNested<!> // should be allowed

    public actual fun publicFun() {} // should be allowed
    public actual fun internalFun() {} // should be allowed
}

public class PlatformOther { // should be allowed
    public class InternalNested // should be allowed
}

<!ACTUAL_WITHOUT_EXPECT!>internal<!> actual typealias Other = PlatformOther // should be allowed

