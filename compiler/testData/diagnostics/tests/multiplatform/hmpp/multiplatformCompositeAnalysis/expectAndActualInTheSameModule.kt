// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: common

expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION!>CommonClass<!><!> {
    fun memberFun()
    val memberProp: Int
    class Nested
    inner class Inner
}
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION!>CommonClass<!><!> {
    actual fun memberFun() {}
    actual val memberProp: Int = 42
    actual class Nested
    actual inner class Inner
}

<!CONFLICTING_OVERLOADS!>expect fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>commonFun<!>()<!>
<!CONFLICTING_OVERLOADS!>actual fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>commonFun<!>()<!> {}

expect val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE, REDECLARATION!>commonProperty<!>: String
actual val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE, REDECLARATION!>commonProperty<!>: String
    get() = "hello"

// MODULE: intermediate()()(common)

expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION!>IntermediateClass<!><!> {
    fun memberFun()
    val memberProp: Int
    class Nested
    inner class Inner
}
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION!>IntermediateClass<!><!> {
    actual fun memberFun() {}
    actual val memberProp: Int = 42
    actual class Nested
    actual inner class Inner
}

<!CONFLICTING_OVERLOADS!>expect fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>intermediateFun<!>()<!>
<!CONFLICTING_OVERLOADS!>actual fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>intermediateFun<!>()<!> {}

expect val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE, REDECLARATION!>intermediateProperty<!>: String
actual val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE, REDECLARATION!>intermediateProperty<!>: String
    get() = "hello"

// MODULE: main()()(intermediate)

expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class PlatformClass<!> {
    fun memberFun()
    val memberProp: Int
    class Nested
    inner class Inner
}
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class PlatformClass<!> {
    actual fun memberFun() {}
    actual val memberProp: Int = 42
    actual class Nested
    actual inner class Inner
}

expect fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>platformFun<!>()
actual fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>platformFun<!>() {}

expect val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>platformProperty<!>: String
actual val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>platformProperty<!>: String
    get() = "hello"
