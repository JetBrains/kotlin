// IGNORE_REVERSED_RESOLVE
import kotlin.reflect.KClass

enum class SomeEnum {
    A, B
}

annotation class MyAnnotation(
    val intValue: Int,
    val stringValue: String,
    val enumValue: SomeEnum,
    val kClasses: Array<out KClass<*>>,
    val annotation: MyOtherAnnotation
)
annotation class MyOtherAnnotation(val intValue: Int, val stringValue: String)

const val constInt = 10
const val constString = ""

@MyAnnotation(
    intValue = 10,
    stringValue = constString,
    enumValue = SomeEnum.A,
    kClasses = [String::class, <!ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL!>constString::class<!>],

    annotation = MyOtherAnnotation(
        intValue = constInt,
        stringValue = "hello"
    )
)
fun foo() {}
