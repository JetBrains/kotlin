//KT-235 Illegal assignment return type

package kt235

fun main() {
    val array = MyArray()
    val f: () -> String = <!INITIALIZER_TYPE_MISMATCH!>{
        array[2] = 23 //error: Type mismatch: inferred type is Int (!!!) but String was expected
    }<!>
    val g: () -> String = <!INITIALIZER_TYPE_MISMATCH!>{
        var x = 1
        x += 2  //no error, but it should be here
    }<!>
    val h: () -> String = <!INITIALIZER_TYPE_MISMATCH!>{
        var x = 1
        x = 2  //the same
    }<!>
    val array1 = MyArray1()
    val i: () -> String = <!INITIALIZER_TYPE_MISMATCH!>{
        array1[2] = 23
    }<!>

    val fi: () -> String = <!INITIALIZER_TYPE_MISMATCH!>{
        array[2] = 23
    }<!>
    val gi: () -> String = <!INITIALIZER_TYPE_MISMATCH!>{
        var x = 1
        x += 21
    }<!>

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
