private fun foo() {}
private inline fun privateFun1() = ::foo
private inline fun privateFun2() = privateFun1()
private inline fun privateFun3() = privateFun2()
internal inline fun internalFun() = privateFun3()
internal inline fun internalFun2() = ::foo