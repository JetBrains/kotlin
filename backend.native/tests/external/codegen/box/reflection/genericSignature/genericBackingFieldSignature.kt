// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT
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

    val delegateLazy: Z<TParam>? by lazy {Z<TParam>()}

    val delegateNotNull: Z<TParam>? by Delegates.notNull()


}

fun box(): String {
    val clz = Test::class.java

    val constructorProperty = clz.getDeclaredField("constructorProperty");

    if (constructorProperty.getGenericType().toString() != "T")
        return "fail0: " + constructorProperty.getGenericType();


    val classField = clz.getDeclaredField("classField1");

    if (classField.getGenericType().toString() != "Z<T>")
        return "fail1: " + classField.getGenericType();


    val classField2 = clz.getDeclaredField("classField2");

    if (classField2.getGenericType().toString() != "Z<java.lang.String>")
        return "fail2: " + classField2.getGenericType();


    val classField3 = clz.getDeclaredField("classField3");

    if (classField3.getGenericType().toString() != "Zout<java.lang.String>")
        return "fail3: " + classField3.getGenericType();


    val classField4 = clz.getDeclaredField("classField4");

    if (classField4.getGenericType().toString() != "Zin<TParam>")
        return "fail4: " + classField4.getGenericType();

    val classField5 = clz.getDeclaredField("delegateLazy\$delegate");

    if (classField5.getGenericType().toString() != "interface kotlin.Lazy")
        return "fail5: " + classField5.getGenericType();

    val classField6 = clz.getDeclaredField("delegateNotNull\$delegate");

    if (classField6.getGenericType().toString() != "interface kotlin.properties.ReadWriteProperty")
        return "fail6: " + classField6.getGenericType();


    return "OK"
}
