fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">foo</info>(<info textAttributesKey="KOTLIN_PARAMETER">p1</info>: <info textAttributesKey="KOTLIN_CLASS">Int</info>, <info textAttributesKey="KOTLIN_PARAMETER">p2</info>: <info textAttributesKey="KOTLIN_CLASS">String</info>): <info textAttributesKey="KOTLIN_CLASS">String</info> {
    return <info textAttributesKey="KOTLIN_PARAMETER">p2</info> + <info textAttributesKey="KOTLIN_PARAMETER">p1</info>
}

fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">bar</info>() {
    <info textAttributesKey="KOTLIN_PACKAGE_FUNCTION_CALL">foo</info>(1, <info textAttributesKey="KOTLIN_NAMED_ARGUMENT">p2 =</info> "")
}
