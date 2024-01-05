// FILE: kotlin.kt
package kotlin.native

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION
)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class ObjCName(val name: String = "", val swiftName: String = "", val exact: Boolean = false)

// FILE: test.kt
@ObjCName("ObjCClass", "SwiftClass")
open class KotlinClass {
    @ObjCName("objCProperty")
    open var kotlinProperty: Int = 0
    @ObjCName(swiftName = "swiftFunction")
    open fun @receiver:ObjCName("objCReceiver") Int.kotlinFunction(
        @ObjCName("objCParam") kotlinParam: Int
    ): Int = this + kotlinParam
}

@ObjCName("ObjCSubClass", "SwiftSubClass")
class KotlinSubClass: KotlinClass() {
    <!INAPPLICABLE_OBJC_NAME!>@ObjCName("objCProperty")<!>
    override var kotlinProperty: Int = 1
    <!INAPPLICABLE_OBJC_NAME!>@ObjCName(swiftName = "swiftFunction")<!>
    override fun <!INAPPLICABLE_OBJC_NAME!>@receiver:ObjCName("objCReceiver")<!> Int.kotlinFunction(
        <!INAPPLICABLE_OBJC_NAME!>@ObjCName("objCParam")<!> kotlinParam: Int
    ): Int = this + kotlinParam * 2
}

<!INVALID_OBJC_NAME!>@ObjCName()<!>
val invalidObjCName: Int = 0

<!EMPTY_OBJC_NAME!>@ObjCName("", "")<!>
val emptyObjCNameA: Int = 0

<!EMPTY_OBJC_NAME!>@ObjCName("validName", "")<!>
val emptyObjCNameB: Int = 0

<!EMPTY_OBJC_NAME!>@ObjCName("", "validName")<!>
val emptyObjCNameC: Int = 0

@ObjCName("validName")
val validObjCNameA: Int = 0

@ObjCName(swiftName = "validName")
val validObjCNameB: Int = 0

<!INVALID_OBJC_NAME_CHARS!>@ObjCName("validName", "invalid.name")<!>
val invalidCharactersObjCNameA: Int = 0

<!INVALID_OBJC_NAME_CHARS!>@ObjCName("invalid.name", "validName")<!>
val invalidCharactersObjCNameB: Int = 0

<!INVALID_OBJC_NAME_FIRST_CHAR!>@ObjCName("validName1", "1validName")<!>
val invalidFirstCharacterObjCNameA: Int = 0

<!INVALID_OBJC_NAME_FIRST_CHAR!>@ObjCName("1validName", "validName1")<!>
val invalidFirstCharacterObjCNameB: Int = 0

<!INVALID_OBJC_NAME_CHARS, INVALID_OBJC_NAME_FIRST_CHAR!>@ObjCName("validName", " ")<!>
val blankObjCNameA: Int = 0

<!INVALID_OBJC_NAME_CHARS, INVALID_OBJC_NAME_FIRST_CHAR!>@ObjCName(" ", "validName")<!>
val blankObjCNameB: Int = 0

<!MISSING_EXACT_OBJC_NAME!>@ObjCName(swiftName = "SwiftMissingExactName", exact = true)<!>
class MissingExactName

interface KotlinInterfaceA {
    @ObjCName("objCPropertyA", "swiftPropertyA")
    var kotlinPropertyA: Int
    @ObjCName("objCPropertyB", "swiftPropertyB")
    var kotlinPropertyB: Int
    @ObjCName("objCPropertyB")
    var kotlinPropertyC: Int
    @ObjCName(swiftName ="swiftPropertyB")
    var kotlinPropertyD: Int
    var kotlinPropertyE: Int
    var kotlinPropertyF: Int

    @ObjCName("objCFunctionA", "swiftFunctionA")
    fun @receiver:ObjCName("objCReceiver", "swiftReceiver") Int.kotlinFunctionA(
        @ObjCName("objCParam", "swiftParam") kotlinParam: Int
    ): Int
    @ObjCName("objCFunctionB", "swiftFunctionB")
    fun @receiver:ObjCName("objCReceiver", "swiftReceiver") Int.kotlinFunctionB(
        @ObjCName("objCParam", "swiftParam") kotlinParam: Int
    ): Int
    @ObjCName("objCFunctionC", "swiftFunctionC")
    fun @receiver:ObjCName("objCReceiver", "swiftReceiver") Int.kotlinFunctionC(
        @ObjCName("objCParam", "swiftParam") kotlinParam: Int
    ): Int
    @ObjCName("objCFunctionD", "swiftFunctionD")
    fun @receiver:ObjCName("objCReceiver", "swiftReceiver") Int.kotlinFunctionD(
        @ObjCName("objCParam", "swiftParam") kotlinParam: Int
    ): Int
    @ObjCName("objCFunctionE", "swiftFunctionE")
    fun Int.kotlinFunctionE(@ObjCName("objCParam", "swiftParam") kotlinParam: Int): Int
}

interface KotlinInterfaceB {
    @ObjCName("objCPropertyA", "swiftPropertyA")
    var kotlinPropertyA: Int
    @ObjCName("objCPropertyBB", "swiftPropertyB")
    var kotlinPropertyB: Int
    @ObjCName(swiftName ="swiftPropertyC")
    var kotlinPropertyC: Int
    @ObjCName("objCPropertyD")
    var kotlinPropertyD: Int
    @ObjCName("objCPropertyE")
    var kotlinPropertyE: Int
    var kotlinPropertyF: Int

    @ObjCName("objCFunctionA", "swiftFunctionA")
    fun @receiver:ObjCName("objCReceiver", "swiftReceiver") Int.kotlinFunctionA(
        @ObjCName("objCParam", "swiftParam") kotlinParam: Int
    ): Int
    @ObjCName("objCFunctionBB", "swiftFunctionB")
    fun @receiver:ObjCName("objCReceiver", "swiftReceiver") Int.kotlinFunctionB(
        @ObjCName("objCParam", "swiftParam") kotlinParam: Int
    ): Int
    @ObjCName("objCFunctionC", "swiftFunctionC")
    fun @receiver:ObjCName("objCReceiverC", "swiftReceiver") Int.kotlinFunctionC(
        @ObjCName("objCParam", "swiftParam") kotlinParam: Int
    ): Int
    @ObjCName("objCFunctionD", "swiftFunctionD")
    fun @receiver:ObjCName("objCReceiver", "swiftReceiver") Int.kotlinFunctionD(
        @ObjCName("objCParamD", "swiftParam") kotlinParam: Int
    ): Int
    fun @receiver:ObjCName("objCFunctionE", "swiftFunctionE") Int.kotlinFunctionE(
        @ObjCName("objCParam", "swiftParam") kotlinParam: Int
    ): Int
}

class KotlinOverrideClass: KotlinInterfaceA, KotlinInterfaceB {
    override var kotlinPropertyA: Int = 0
    <!INCOMPATIBLE_OBJC_NAME_OVERRIDE!>override var kotlinPropertyB: Int = 0<!>
    <!INCOMPATIBLE_OBJC_NAME_OVERRIDE!>override var kotlinPropertyC: Int = 0<!>
    <!INCOMPATIBLE_OBJC_NAME_OVERRIDE!>override var kotlinPropertyD: Int = 0<!>
    <!INCOMPATIBLE_OBJC_NAME_OVERRIDE!>override var kotlinPropertyE: Int = 0<!>
    override var kotlinPropertyF: Int = 0

    override fun Int.kotlinFunctionA(kotlinParam: Int): Int = this + kotlinParam
    <!INCOMPATIBLE_OBJC_NAME_OVERRIDE!>override fun Int.kotlinFunctionB(kotlinParam: Int): Int = this + kotlinParam<!>
    <!INCOMPATIBLE_OBJC_NAME_OVERRIDE!>override fun Int.kotlinFunctionC(kotlinParam: Int): Int = this + kotlinParam<!>
    <!INCOMPATIBLE_OBJC_NAME_OVERRIDE!>override fun Int.kotlinFunctionD(kotlinParam: Int): Int = this + kotlinParam<!>
    <!INCOMPATIBLE_OBJC_NAME_OVERRIDE!>override fun Int.kotlinFunctionE(kotlinParam: Int): Int = this + kotlinParam<!>
}

@ObjCName("ObjCExactChecks", exact = true)
class ExactChecks {
    <!INAPPLICABLE_EXACT_OBJC_NAME!>@ObjCName("objCProperty", exact = true)<!>
    var property: Int = 0
    <!INAPPLICABLE_EXACT_OBJC_NAME!>@ObjCName("objCFunction", exact = true)<!>
    fun <!INAPPLICABLE_EXACT_OBJC_NAME!>@receiver:ObjCName("objCReceiver", exact = true)<!> Int.function(
        <!INAPPLICABLE_EXACT_OBJC_NAME!>@ObjCName("objCParam", exact = true)<!> param: Int
    ): Int = this * param
}

@ObjCName("ObjCEnumExactChecks", exact = true)
enum class EnumExactChecks {
    <!INAPPLICABLE_EXACT_OBJC_NAME!>@ObjCName("objCEntryOne", exact = true)<!>
    ENTRY_ONE,
    @ObjCName("objCEntryTwo")
    ENTRY_TWO
}

open class Base {
    @ObjCName("foo1")
    open fun foo() {}
}

interface I {
    @ObjCName("foo2")
    fun foo()
}

<!INCOMPATIBLE_OBJC_NAME_OVERRIDE!>open class Derived : Base(), I<!>

open class Derived2 : Derived() {
    override fun foo() {}
}

interface I1 {
    @ObjCName("foo1")
    fun foo()
}

interface DerivedI1 : I1 {
    override fun foo()
}

abstract class KT64276 : Base(), DerivedI1 {}

private const val exact = false
private const val objcName = "nonLiteralArgsObjC"

<!INVALID_OBJC_NAME!>@ObjCName(
    <!NON_LITERAL_OBJC_NAME_ARG!>objcName<!>,
    <!NON_LITERAL_OBJC_NAME_ARG!>"nonLiteralArgs" + "Swift"<!>,
    <!NON_LITERAL_OBJC_NAME_ARG!>exact<!>
)<!>
val nonLiteralArgs: Int = 0

@ObjCName("invalidArgsObjC", <!ARGUMENT_TYPE_MISMATCH!>false<!>, <!ARGUMENT_TYPE_MISMATCH!>"not a boolean"<!>)
val invalidArgs: Int = 0
