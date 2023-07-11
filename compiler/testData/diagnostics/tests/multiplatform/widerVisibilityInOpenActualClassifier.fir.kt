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
public actual open <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER!>class Some<!> { // should be allowed
    public class ProtectedNested  // should be allowed
    public class InternalNested // should be allowed

    public actual fun publicFun() {} // should be allowed
    <!VISIBILITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION!>public<!> actual fun internalFun() {} // shouldn't be allowed
    <!VISIBILITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION!>public<!> actual fun protectedFun() {} // shouldn't be allowed
}

public open class PlatformOther { // should be allowed
    public class ProtectedNested  // should be allowed
    public class InternalNested // should be allowed
}

internal actual typealias Other = PlatformOther // should be allowed

