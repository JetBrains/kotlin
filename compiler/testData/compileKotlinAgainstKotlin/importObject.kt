// FILE: A.kt

package some

public object SOME_OBJECT {

}

// FILE: B.kt

//This a test for blinking bug from KT-3124

import some.SOME_OBJECT

fun main(args: Array<String>) {
    val a = SOME_OBJECT
}
