// EXPECTED_DUPLICATED_HIGHLIGHTING
// FALSE_POSITIVE

var <info textAttributesKey="KOTLIN_PACKAGE_PROPERTY"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE">my</info></info> = 0
    <info textAttributesKey="KOTLIN_KEYWORD">get</info>() = <info textAttributesKey="KOTLIN_BACKING_FIELD_VARIABLE"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE">field</info></info>
    <info textAttributesKey="KOTLIN_KEYWORD">set</info>(<info textAttributesKey="KOTLIN_PARAMETER">arg</info>) {
        <info textAttributesKey="KOTLIN_BACKING_FIELD_VARIABLE"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE">field</info></info> = <info textAttributesKey="KOTLIN_PARAMETER">arg</info> + 1
    }

fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">foo</info>(): <info textAttributesKey="KOTLIN_CLASS">Int</info> {
    val <info textAttributesKey="KOTLIN_LOCAL_VARIABLE">field</info> = <info textAttributesKey="KOTLIN_PACKAGE_PROPERTY"><info textAttributesKey="KOTLIN_MUTABLE_VARIABLE">my</info></info>
    return <info textAttributesKey="KOTLIN_LOCAL_VARIABLE">field</info>
}