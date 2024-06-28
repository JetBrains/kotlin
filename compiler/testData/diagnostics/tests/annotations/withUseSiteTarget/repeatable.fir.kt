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

    @Ann @field:Ann <!REPEATED_ANNOTATION!>@property:Ann<!>
    val b: Int = 0

    @field:RepeatableAnn @field:RepeatableAnn
    val c: Int = 0

    @property:RepeatableAnn @RepeatableAnn
    val d: Int = 0

    @property:RepeatableAnn @RepeatableAnn @delegate:RepeatableAnn
    val e: String by CustomDelegate()

    @property:Ann @delegate:Ann
    val f: String by CustomDelegate()

    @Ann @delegate:Ann
    val g: String by CustomDelegate()

    @Ann @field:Ann
    val h: String = ""

    @property:Ann @field:Ann
    val i: String = ""
}

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class fieldOrPropAnn

@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER)
annotation class getSetAndParamAnn

public class B(<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@param:fieldOrPropAnn<!> @fieldOrPropAnn val x: Int,
               @property:fieldOrPropAnn <!REPEATED_ANNOTATION!>@fieldOrPropAnn<!> val y: Int) {
    @fieldOrPropAnn @field:fieldOrPropAnn
    val z: Int = 42

    <!WRONG_ANNOTATION_TARGET!>@getSetAndParamAnn<!>
    @setparam:getSetAndParamAnn
    var w: Int
        @getSetAndParamAnn <!INAPPLICABLE_TARGET_ON_PROPERTY, REPEATED_ANNOTATION!>@get:getSetAndParamAnn<!> get() = 0
        // See KT-15470: fake INAPPLICABLE_TARGET_ON_PROPERTY
        @getSetAndParamAnn <!INAPPLICABLE_TARGET_ON_PROPERTY, REPEATED_ANNOTATION!>@set:getSetAndParamAnn<!> set(arg) {}
}
