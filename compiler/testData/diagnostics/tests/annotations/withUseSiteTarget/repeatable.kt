import kotlin.reflect.KProperty

@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class RepeatableAnn
annotation class Ann

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

public class A(@param:Ann <!REPEATED_ANNOTATION!>@Ann<!> val x: Int, @param: RepeatableAnn @Ann val y: Int) {

    @field:Ann @property:Ann @RepeatableAnn @property:RepeatableAnn
    val a: Int = 0

    @Ann <!REPEATED_ANNOTATION!>@field:Ann<!> <!REPEATED_ANNOTATION!>@property:Ann<!>
    val b: Int = 0

    @field:RepeatableAnn @field:RepeatableAnn
    val c: Int = 0

    @property:RepeatableAnn @RepeatableAnn
    val d: Int = 0

    @property:RepeatableAnn @RepeatableAnn @delegate:RepeatableAnn
    val e: String by CustomDelegate()

    @property:Ann @delegate:Ann
    val f: String by CustomDelegate()

    // Ideally we should not have repeated anotation here and below
    // (because @Ann should go to the property and the second annotation to its related field)
    @Ann <!REPEATED_ANNOTATION!>@delegate:Ann<!>
    val g: String by CustomDelegate()

    @Ann <!REPEATED_ANNOTATION!>@field:Ann<!>
    val h: String = ""

    @property:Ann @field:Ann
    val i: String = ""
}