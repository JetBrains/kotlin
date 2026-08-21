import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KCallable
import kotlin.reflect.KProperty

public interface MyRwProperty<in T, V> {
    public operator fun setValue(thisRef: T, property: Any, value: V)
    public operator fun getValue(thisRef: T, property: Any): V
}

val x: Int by lazy { 1 + 2 }

val delegate = object: MyRwProperty<Any?, Int> {
    override fun getValue(thisRef: Any?, property: Any): Int = 1
    override fun setValue(thisRef: Any?, property: Any, value: Int) {}
}

val value by delegate

var variable by delegate
