package testing

object <info textAttributesKey="KOTLIN_CLASS">O</info> {
    fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">foo</info>() = 42
}

fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">testing</info>(): <info textAttributesKey="KOTLIN_OBJECT">O</info> {
    <info textAttributesKey="KOTLIN_OBJECT">O</info>.<info textAttributesKey="KOTLIN_FUNCTION_CALL">foo</info>()
    val <info textAttributesKey="KOTLIN_LOCAL_VARIABLE">o</info> = <info textAttributesKey="KOTLIN_OBJECT">O</info>
    <info textAttributesKey="KOTLIN_LOCAL_VARIABLE">o</info>.<info textAttributesKey="KOTLIN_FUNCTION_CALL">foo</info>()
    return <info textAttributesKey="KOTLIN_OBJECT">O</info>
}
