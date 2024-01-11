// IGNORE_TREE_ACCESS: KT-64898
@Anno("x annotation $x")
val x: Int by lazy { 1 + 2 }

@Anno("delegate annotation $x")
@delegate:Anno("delegate: delegate annotation $x")
val delegate = object: ReadWriteProperty<@Anno("ReadWriteProperty first type argument $x") Any?,  @Anno("ReadWriteProperty second type argument $x") Int> {
    @Anno("getValue $x")
    override fun getValue(thisRef: Any?, property: KProperty<*>): @Anno("getValue return type $x") Int = 1

    @Anno("setValue $x")
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: @Anno("setValue value parameter type $x") Int) {}
}

@Anno("value annotation $x")
@delegate:Anno("delegate: value annotation $x")
@get:Anno("get: value annotation $x")
val value by delegate

@Anno("variable annotation $x")
@delegate:Anno("delegate: value annotation $x")
@get:Anno("get: value annotation $x")
@set:Anno("set: value annotation $x")
@setparam:Anno("setparam: value annotation $x")
var variable by delegate
