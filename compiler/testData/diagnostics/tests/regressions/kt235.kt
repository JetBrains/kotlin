//KT-235 Illegal assignment return type

package kt235

fun main(args: Array<String>) {
    val array = MyArray()
    val <!UNUSED_VARIABLE!>f<!> = { (): String ->
       <!EXPECTED_TYPE_MISMATCH!>array[2] = 23<!> //error: Type mismatch: inferred type is Int (!!!) but String was expected
    }
    val <!UNUSED_VARIABLE!>g<!> = {(): String ->
       var x = 1
       <!EXPECTED_TYPE_MISMATCH!>x += 2<!>  //no error, but it should be here
    }
    val <!UNUSED_VARIABLE!>h<!> = {(): String ->
       var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>x<!> = 1
       <!EXPECTED_TYPE_MISMATCH!>x = <!UNUSED_VALUE!>2<!><!>  //the same
    }
    val array1 = MyArray1()
    val <!UNUSED_VARIABLE!>i<!> = { (): String ->
       <!EXPECTED_TYPE_MISMATCH!>array1[2] = 23<!>
    }

    val <!UNUSED_VARIABLE!>fi<!> = { (): Int ->
       <!ASSIGNMENT_TYPE_MISMATCH!>array[2] = 23<!>
    }
    val <!UNUSED_VARIABLE!>gi<!> = {(): Int ->
       var x = 1
       <!ASSIGNMENT_TYPE_MISMATCH!>x += 21<!>
    }

    var m: MyNumber = MyNumber()
    val <!UNUSED_VARIABLE!>a<!> = { (): MyNumber ->
        m++
    }
}

class MyArray() {
    fun get(i: Int): Int = 1
    fun set(i: Int, value: Int): Int = 1
}

class MyArray1() {
    fun get(i: Int): Int = 1
    fun set(i: Int, value: Int) {}
}

class MyNumber() {
    fun inc(): MyNumber = MyNumber()
}