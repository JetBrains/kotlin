var <info textAttributesKey="KOTLIN_NAMESPACE_PROPERTY"><info textAttributesKey="KOTLIN_PROPERTY_WITH_BACKING_FIELD"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE">global</info></info></info> : () -> <info textAttributesKey="KOTLIN_CLASS">Unit</info> = {}

val <info textAttributesKey="KOTLIN_CLASS">Int</info>.<info textAttributesKey="KOTLIN_EXTENSION_PROPERTY"><info textAttributesKey="KOTLIN_NAMESPACE_PROPERTY">ext</info></info> : () -> <info textAttributesKey="KOTLIN_CLASS">Unit</info>
<info textAttributesKey="KOTLIN_KEYWORD">get</info>() {
  return {}
}

fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">foo</info>(<info textAttributesKey="KOTLIN_PARAMETER">a</info> : () -> <info textAttributesKey="KOTLIN_CLASS">Unit</info>) {
    <info textAttributesKey="KOTLIN_PARAMETER">a</info>()
    <info textAttributesKey="KOTLIN_NAMESPACE_PROPERTY"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE">global</info></info>()
    1.<info textAttributesKey="KOTLIN_EXTENSION_PROPERTY"><info textAttributesKey="KOTLIN_NAMESPACE_PROPERTY">ext</info></info>()
}