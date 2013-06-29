//test for KT-3722 Write correct generic type information for generated fields
import kotlin.properties.Delegates

class Z<T> {

}

class TParam {

}

class Zout<out T> {

}

class Zin<in T> {

}


class Test<T>(val constructorProperty: T) {

    val classField1 : Z<T>? = null

    val classField2 : Z<String>? = null

    val classField3 : Zout<String>? = null

    val classField4 : Zin<TParam>? = null

    val delegates : Z<TParam>? by Delegates.lazy {Z<TParam>()}


}

fun box(): String {
    val clz = javaClass<Test<*>>()

    val constructorProperty = clz.getDeclaredField("constructorProperty");

    if (constructorProperty.getGenericType().toString() != "T")
        return "fail0: " + constructorProperty.getGenericType();


    val classField = clz.getDeclaredField("classField1");

    if (classField.getGenericType().toString() != "Z<T>")
        return "fail1:" + classField.getGenericType();


    val classField2 = clz.getDeclaredField("classField2");

    if (classField2.getGenericType().toString() != "Z<java.lang.String>")
        return "fail2:" + classField2.getGenericType();


    val classField3 = clz.getDeclaredField("classField3");

    if (classField3.getGenericType().toString() != "Zout<? extends java.lang.String>")
        return "fail3:" + classField3.getGenericType();


    val classField4 = clz.getDeclaredField("classField4");

    if (classField4.getGenericType().toString() != "Zin<? super TParam>")
        return "fail4:" + classField4.getGenericType();

    val classField5 = clz.getDeclaredField("delegates\$delegate");

    if (classField5.getGenericType().toString() != "kotlin.properties.ReadOnlyProperty<? super java.lang.Object, ? extends Z<TParam>>")
        return "fail5:" + classField5.getGenericType();


    return "OK"
}
