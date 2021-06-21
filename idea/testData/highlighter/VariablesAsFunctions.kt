// IGNORE_FIR
// EXPECTED_DUPLICATED_HIGHLIGHTING

interface <info textAttributesKey="KOTLIN_TRAIT">FunctionLike</info> {
    <info descr="null" textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">operator</info> fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">invoke</info>() {
    }
}

var <info textAttributesKey="KOTLIN_PACKAGE_PROPERTY"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE">global</info></info> : () -> <info textAttributesKey="KOTLIN_OBJECT">Unit</info> = {}

val <info textAttributesKey="KOTLIN_CLASS">Int</info>.<info textAttributesKey="KOTLIN_EXTENSION_PROPERTY">ext</info> : () -> <info textAttributesKey="KOTLIN_OBJECT">Unit</info>
<info textAttributesKey="KOTLIN_KEYWORD">get</info>() {
  return {}
}

fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">foo</info>(<info textAttributesKey="KOTLIN_PARAMETER">a</info> : () -> <info textAttributesKey="KOTLIN_OBJECT">Unit</info>, <info textAttributesKey="KOTLIN_PARAMETER">functionLike</info>: <info textAttributesKey="KOTLIN_TRAIT">FunctionLike</info>) {
    <info textAttributesKey="KOTLIN_PARAMETER"><info textAttributesKey="KOTLIN_VARIABLE_AS_FUNCTION">a</info></info>()
    <info textAttributesKey="KOTLIN_PARAMETER"><info textAttributesKey="KOTLIN_VARIABLE_AS_FUNCTION_LIKE">functionLike</info></info>()
    <info textAttributesKey="KOTLIN_PACKAGE_PROPERTY"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE"><info textAttributesKey="KOTLIN_VARIABLE_AS_FUNCTION">global</info></info></info>()
    1.<info textAttributesKey="KOTLIN_EXTENSION_PROPERTY"><info textAttributesKey="KOTLIN_VARIABLE_AS_FUNCTION">ext</info></info>();

    {}() //should not be highlighted as "calling variable as function"!
}
