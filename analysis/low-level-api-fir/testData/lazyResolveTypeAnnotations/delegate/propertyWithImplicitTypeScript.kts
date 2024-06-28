// SKIP_WHEN_OUT_OF_CONTENT_ROOT

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Target(
    AnnotationTarget.TYPE,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.TYPE_PARAMETER,
)
annotation class Anno(val position: String)

const val prop = 0

class Delegate: ReadWriteProperty<Any?, @Anno("super type ref $prop") List<@Anno("nested super type ref $prop") List<@Anno("nested nested super type ref $prop") Int>>> {
    fun explicitType(): @Anno("explicitType return type $prop") List<@Anno("explicitType nested return type $prop") List<@Anno("explicitType nested nested return type $prop") Int>> = 1
    override fun getValue(thisRef: Any?, property: KProperty<*>) = explicitType()
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: @Anno("setValue type ref $prop") List<@Anno("setValue nested type ref $prop") List<@Anno("setValue nested nested type ref $prop") Int>>) {}
}

@property:Anno("property $prop")
@delegate:Anno("delegate $prop")
@get:Anno("get $prop")
@set:Anno("set $prop")
@setparam:Anno("setparam $prop")
var <@Anno("type param $prop") F : @Anno("bound $prop") List<@Anno("nested bound $prop") List<@Anno("nested nested bound $prop") String>>> @receiver:Anno("receiver $prop") F.property<caret>ToResolve by Delegate()
