package myPack

const val myNumber = 1

fun top<caret>LevelFunction(firstParam: Int, secondParam: String = "My str ${firstParam.plus(myNumber).toString()}") = 42