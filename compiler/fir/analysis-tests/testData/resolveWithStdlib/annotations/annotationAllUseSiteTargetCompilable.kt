// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73256, KT-75163, KT-75195
// LANGUAGE: +AnnotationAllUseSiteTarget

package p

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ParamOnly(val x: Int = 0)

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
annotation class ParamProperty(val y: Double = 0.0)

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class ParamField(val z: Boolean = false)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class PropertyField(val s: String = "")

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class ParamPropertyField(val c: Char = ' ')

@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class GetterSetter(val x: Int)

@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
annotation class ParamGetter(val y: Double)

@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER)
annotation class ParamGetterSetter(val z: Boolean)

annotation class Default(val s: String)

class My(
    @all:ParamOnly
    @all:ParamProperty
    @all:ParamField
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter(1)
    @all:ParamGetter(1.0)
    @all:ParamGetterSetter(false)
    @all:Default("a")
    val valFromConstructor: Int,

    @all:ParamOnly
    @all:ParamProperty
    @all:ParamField
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter(2)
    @all:ParamGetter(2.0)
    @all:ParamGetterSetter(true)
    @all:Default("b")
    var varFromConstructor: Int,
) {
    @all:ParamProperty
    @all:ParamField
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter(3)
    @all:ParamGetter(3.0)
    @all:ParamGetterSetter(false)
    @all:Default("c")
    val valInside: Int = 0

    @all:ParamOnly
    @all:ParamProperty
    @all:ParamField
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter(4)
    @all:ParamGetter(4.0)
    @all:ParamGetterSetter(true)
    @all:Default("d")
    var varInside: Int = 1

    @all:ParamProperty
    @all:ParamField
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter(5)
    @all:ParamGetter(5.0)
    @all:ParamGetterSetter(false)
    @all:Default("e")
    val valWithGetter: Int = 2
        get() = field

    @all:ParamProperty
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter(6)
    @all:ParamGetter(6.0)
    @all:ParamGetterSetter(true)
    @all:Default("f")
    val valWithoutField: Int
        get() = 3

    @all:ParamOnly
    @all:ParamProperty
    @all:ParamField
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter(7)
    @all:ParamGetter(7.0)
    @all:ParamGetterSetter(false)
    @all:Default("g")
    var varWithSetter: Int = 4
        set(param) {}

    @all:ParamOnly
    @all:ParamProperty
    @all:ParamField
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter(8)
    @all:ParamGetter(8.0)
    @all:ParamGetterSetter(true)
    @all:Default("h")
    var varWithSetterAndGetter: Int = 5
        get() = field
        set(param) {}

    @all:ParamOnly
    @all:ParamProperty
    @all:PropertyField
    @all:ParamPropertyField
    @all:GetterSetter(9)
    @all:ParamGetter(9.0)
    @all:ParamGetterSetter(false)
    @all:Default("i")
    var varWithoutField: Int
        get() = 6
        set(param) {}
}
