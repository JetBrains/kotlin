//KT-235 Illegal assignment return type

package kt235

fun main() {
    val array = MyArray()
    val f: () -> String = {
        <!EXPECTED_TYPE_MISMATCH!>array[2] = 23<!> //error: Type mismatch: inferred type is Int (!!!) but String was expected
    }
    val g: () -> String = {
        var x = 1
        <!EXPECTED_TYPE_MISMATCH!>x += 2<!>  //no error, but it should be here
    }
    val h: () -> String = {
        var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>x<!> = 1
        <!EXPECTED_TYPE_MISMATCH!>x = 2<!>  //the same
    }
    val array1 = MyArray1()
    val i: () -> String = {
        <!EXPECTED_TYPE_MISMATCH!>array1[2] = 23<!>
    }

    val fi: () -> String = {
        <!EXPECTED_TYPE_MISMATCH!>array[2] = 23<!>
    }
    val gi: () -> String = {
        var x = 1
        <!EXPECTED_TYPE_MISMATCH!>x += 21<!>
    }

    var m: MyNumber = MyNumber()
    val a: () -> MyNumber = {
        m++
    }
}

class MyArray() {
    operator fun get(i: Int): Int = 1
    operator fun set(i: Int, value: Int): Int = 1
}

class MyArray1() {
    operator fun get(i: Int): Int = 1
    operator fun set(i: Int, value: Int) {}
}

class MyNumber() {
    operator fun inc(): MyNumber = MyNumber()
}
