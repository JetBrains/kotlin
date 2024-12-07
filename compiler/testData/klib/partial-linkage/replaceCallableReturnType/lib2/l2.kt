fun doCommonViaProperty() = topLevelProperty.doCommon()
fun doSpecificViaProperty() = topLevelProperty.doSpecificFoo() // Because, initially topLevelProperty had type Foo.

fun doCommonViaFunction() = topLevelFunction().doCommon()
fun doSpecificViaFunction() = topLevelFunction().doSpecificFoo() // Because, initially topLevelFunction had return type Foo.
