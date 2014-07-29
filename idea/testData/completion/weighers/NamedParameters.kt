package test

import temp.test.*

val listThisFileVal = 12
fun listThisFileFun() = 1

class ListThisFileClass {}

fun test(listParam: Int) {
    val listLocalVal = 12
    Options(list<caret>)
}

// ORDER: listLocalVal
// ORDER: listParam
// ORDER: listThisFileVal
// ORDER: listImportedVal
// ORDER: listThisFileFun
// ORDER: listImportedFun
// ORDER: listMatch
// ORDER: listNew
