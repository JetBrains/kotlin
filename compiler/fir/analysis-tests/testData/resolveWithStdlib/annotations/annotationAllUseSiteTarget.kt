// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73256
// LANGUAGE: +AnnotationAllUseSiteTarget
// FIR_DUMP

@<!INAPPLICABLE_ALL_TARGET!>all<!>:Default
package p

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
annotation class ParamProperty

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class ParamField

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class PropertyField

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class ParamPropertyField

@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class GetterSetter

@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
annotation class ParamGetter

@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER)
annotation class ParamGetterSetter

annotation class Default

@Target(AnnotationTarget.CLASS)
annotation class Inapplicable

@<!INAPPLICABLE_ALL_TARGET!>all<!>:Default
class My(
    @all:ParamProperty
    @all:ParamField
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter
    @all:ParamGetter
    @all:ParamGetterSetter
    @all:Default
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:Inapplicable<!>
    val valFromConstructor: Int,

    @all:ParamProperty
    @all:ParamField
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter
    @all:ParamGetter
    @all:ParamGetterSetter
    @all:Default
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:Inapplicable<!>
    var varFromConstructor: Int,

    @<!INAPPLICABLE_ALL_TARGET!>all<!>:Default
    param: Int,
) {
    @all:ParamProperty
    @all:ParamField
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter
    @all:ParamGetter
    @all:ParamGetterSetter
    @all:Default
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:Inapplicable<!>
    val valInside: Int = 0

    @all:ParamProperty
    @all:ParamField
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter
    @all:ParamGetter
    @all:ParamGetterSetter
    @all:Default
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:Inapplicable<!>
    var varInside: Int = 1

    @all:ParamProperty
    @all:ParamField
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter
    @all:ParamGetter
    @all:ParamGetterSetter
    @all:Default
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:Inapplicable<!>
    val valWithGetter: Int = 2
        get() = field

    @all:ParamProperty
    <!WRONG_ANNOTATION_TARGET!>@all:ParamField<!>
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter
    @all:ParamGetter
    @all:ParamGetterSetter
    @all:Default
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:Inapplicable<!>
    val valWithoutField: Int
        get() = 3

    @all:ParamProperty
    @all:ParamField
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter
    @all:ParamGetter
    @all:ParamGetterSetter
    @all:Default
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:Inapplicable<!>
    var varWithSetter: Int = 4
        set(param) {}

    @all:ParamProperty
    @all:ParamField
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter
    @all:ParamGetter
    @all:ParamGetterSetter
    @all:Default
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:Inapplicable<!>
    var varWithSetterAndGetter: Int = 5
        get() = field
        set(param) {}

    @all:ParamProperty
    <!WRONG_ANNOTATION_TARGET!>@all:ParamField<!>
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter
    @all:ParamGetter
    @all:ParamGetterSetter
    @all:Default
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:Inapplicable<!>
    var varWithoutField: Int
        get() = 6
        set(param) {}

    @all:ParamProperty
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:ParamField<!>
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter
    @all:ParamGetter
    @all:ParamGetterSetter
    @all:Default
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:Inapplicable<!>
    val delegatedVal: Int by lazy { 7 }

    @<!INAPPLICABLE_ALL_TARGET!>all<!>:Default
    fun foo(@<!INAPPLICABLE_ALL_TARGET!>all<!>:Default param: Int): <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:Default<!> Int {
        @<!INAPPLICABLE_ALL_TARGET!>all<!>:Default val x = 8
        val y = <!WRONG_ANNOTATION_TARGET!>@all:Default<!> x
        return y
    }

    @<!INAPPLICABLE_ALL_TARGET!>all<!>:Default
    constructor(@<!INAPPLICABLE_ALL_TARGET!>all<!>:Default param: Int): this(0, 1, 2)

    var withIllegal: Int = 9
        @<!INAPPLICABLE_ALL_TARGET!>all<!>:Default get() = field
        @<!INAPPLICABLE_ALL_TARGET!>all<!>:Default set(@<!INAPPLICABLE_ALL_TARGET!>all<!>:Default param) {}
}
