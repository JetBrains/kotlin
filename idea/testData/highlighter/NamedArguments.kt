// IGNORE_FIR
<info descr="null" textAttributesKey="KOTLIN_ANNOTATION">@Suppress</info>(<info descr="null" textAttributesKey="KOTLIN_ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES">names =</info> ["foo"])
fun <info descr="null" textAttributesKey="KOTLIN_FUNCTION_DECLARATION">foo</info>(<info descr="null" textAttributesKey="KOTLIN_PARAMETER">p1</info>: <info descr="null" textAttributesKey="KOTLIN_CLASS">Int</info>, <info descr="null" textAttributesKey="KOTLIN_PARAMETER">p2</info>: <info descr="null" textAttributesKey="KOTLIN_CLASS">String</info>): <info descr="null" textAttributesKey="KOTLIN_CLASS">String</info> {
    return <info descr="null" textAttributesKey="KOTLIN_PARAMETER">p2</info> + <info descr="null" textAttributesKey="KOTLIN_PARAMETER">p1</info>
}

fun <info descr="null" textAttributesKey="KOTLIN_FUNCTION_DECLARATION">bar</info>() {
    <info descr="null" textAttributesKey="KOTLIN_PACKAGE_FUNCTION_CALL">foo</info>(1, <info descr="null" textAttributesKey="KOTLIN_NAMED_ARGUMENT">p2 =</info> "")
}