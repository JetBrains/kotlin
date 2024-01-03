// one.UTypeDeclarationClass
// WITH_STDLIB
package one

@JvmInline
value class MyValueClass(val str: String)

class UTypeDeclarationClass {
    var setterAndGetter: MyValueClass = MyValueClass("setterAndGetter")
        @JvmName("get_setterAndGetter") get() = field
        @JvmName("set_setterAndGetter") set(value) { field = value }

    var setter: MyValueClass = MyValueClass("setter")
        get() = field
        @JvmName("set_setter") set(value) { field = value }

    var getter: MyValueClass = MyValueClass("getter")
        @JvmName("get_getter") get() = field
        set(value) { field = value }

    var nothing: MyValueClass = MyValueClass("nothing")
        get() = field
        set(value) { field = value }

    fun methodWithValueClass(p: MyValueClass) {}

    @JvmName("_methodWithJvmName")
    fun methodWithJvmName(p: MyValueClass) {}
}
