package lowlevel

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

fun <F : @Anno("bound $prop") ASF> @Anno("receiver $prop") Abc.func<caret>tion(param: @Anno("param $prop") Type1<@Anno("nested param $prop") Type2>): @Anno("return $prop") Type3 {}