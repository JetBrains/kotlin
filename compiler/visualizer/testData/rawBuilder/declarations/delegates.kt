import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

//            fun <T> lazy<Int>(() -> T): Lazy<T>
//            │      Int
//            │      │ fun (Int).plus(Int): Int
//  Int       │      │ │ Int
//  │         │      │ │ │
val x: Int by lazy { 1 + 2 }

//  properties/ReadWriteProperty<Any?, Int>
//  │                  properties/ReadWriteProperty<Any?, Int>
//  │                  │
val delegate = object: ReadWriteProperty<Any?, Int> {
//                                                 reflect/KProperty<*>
//                                                 │                  Int
//                                                 │                  │ Int
//                                                 │                  │ │
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int = 1
//                                                 reflect/KProperty<*>
//                                                 │
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {}
}

//  Int      val delegate: properties/ReadWriteProperty<Any?, Int>
//  │        │
val value by delegate

//  Int         val delegate: properties/ReadWriteProperty<Any?, Int>
//  │           │
var variable by delegate
