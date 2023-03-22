// IGNORE_REVERSED_RESOLVE
import kotlin.reflect.KClass

@Repeatable
annotation class Ann(val a: Array<KClass<*>>)

class Foo

val foo = Foo::class
fun bar() = Foo::class

@Ann(
    [
        <!ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL!>""::class<!>,
        <!ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL!>true::class<!>,
        <!ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL!>1::class<!>
    ]
)
@Ann(
    [
        <!ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL!>foo<!>,
        <!ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL!>bar()<!>
    ]
)
fun test1() {}
