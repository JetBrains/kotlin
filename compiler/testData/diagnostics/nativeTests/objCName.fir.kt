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
    @ObjCName("objCProperty")
    override var kotlinProperty: Int = 1
    @ObjCName(swiftName = "swiftFunction")
    override fun @receiver:ObjCName("objCReceiver") Int.kotlinFunction(
        @ObjCName("objCParam") kotlinParam: Int
    ): Int = this + kotlinParam * 2
}

@ObjCName()
val invalidObjCName: Int = 0

@ObjCName("validName", "invalid.name")
val invalidCharactersObjCNameA: Int = 0

@ObjCName("invalid.name", "validName")
val invalidCharactersObjCNameB: Int = 0

@ObjCName("validName1", "1validName")
val invalidFirstCharacterObjCNameA: Int = 0

@ObjCName("1validName", "validName1")
val invalidFirstCharacterObjCNameB: Int = 0

@ObjCName(swiftName = "SwiftMissingExactName", exact = true)
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
    override var kotlinPropertyB: Int = 0
    override var kotlinPropertyC: Int = 0
    override var kotlinPropertyD: Int = 0
    override var kotlinPropertyE: Int = 0
    override var kotlinPropertyF: Int = 0

    override fun Int.kotlinFunctionA(kotlinParam: Int): Int = this + kotlinParam
    override fun Int.kotlinFunctionB(kotlinParam: Int): Int = this + kotlinParam
    override fun Int.kotlinFunctionC(kotlinParam: Int): Int = this + kotlinParam
    override fun Int.kotlinFunctionD(kotlinParam: Int): Int = this + kotlinParam
    override fun Int.kotlinFunctionE(kotlinParam: Int): Int = this + kotlinParam
}

@ObjCName("ObjCExactChecks", exact = true)
class ExactChecks {
    @ObjCName("objCProperty", exact = true)
    var property: Int = 0
    @ObjCName("objCFunction", exact = true)
    fun @receiver:ObjCName("objCReceiver", exact = true) Int.function(
        @ObjCName("objCParam", exact = true) param: Int
    ): Int = this * param
}

@ObjCName("ObjCEnumExactChecks", exact = true)
enum class EnumExactChecks {
    @ObjCName("objCEntryOne", exact = true)
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

open class Derived : Base(), I

open class Derived2 : Derived() {
    override fun foo() {}
}

private const val exact = false
private const val objcName = "nonLiteralArgsObjC"

@ObjCName(
    objcName,
    "nonLiteralArgs" + "Swift",
    exact
)
val nonLiteralArgs: Int = 0

@ObjCName("invalidArgsObjC", <!ARGUMENT_TYPE_MISMATCH!>false<!>, <!ARGUMENT_TYPE_MISMATCH!>"not a boolean"<!>)
val invalidArgs: Int = 0
