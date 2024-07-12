// one.ValueClassInParametersWithJvmNameKt
// WITH_STDLIB
package one

@JvmInline
value class MyValueClass(val str: String)

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

fun functionWithValueClassInReturn(): MyValueClass? = null

@JvmName("_functionWithValueClassInReturnWithJvmName")
fun functionWithValueClassInReturnWithJvmName(): MyValueClass? = null

fun MyValueClass.functionWithValueClassInReceiver() {}

@JvmName("_functionWithValueClassInReceiverWithJvmName")
fun MyValueClass.functionWithValueClassInReceiverWithJvmName() {}
