// PROBLEM: none

import TestClass.NamedObject.CONST

class TestClass{
    fun method(){
        CONST
    }

    private object NamedObject<caret> {
        const val CONST = "abc"
    }
}

fun main(){
    TestClass().method()
}