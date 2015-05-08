package testing

<info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">enum</info> class <info textAttributesKey="KOTLIN_CLASS">Test</info> {
    <info textAttributesKey="KOTLIN_CLASS">FIRST</info>,
    <info textAttributesKey="KOTLIN_CLASS">SECOND</info>
}

fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">testing</info>(<info textAttributesKey="KOTLIN_PARAMETER">t1</info>: <info textAttributesKey="KOTLIN_CLASS">Test</info>, <info textAttributesKey="KOTLIN_PARAMETER">t2</info>: <info textAttributesKey="KOTLIN_CLASS">Test</info>): <info textAttributesKey="KOTLIN_CLASS">Test</info> {
    if (<info textAttributesKey="KOTLIN_PARAMETER">t1</info> != <info textAttributesKey="KOTLIN_PARAMETER">t2</info>) return <info textAttributesKey="KOTLIN_CLASS">Test</info>.<info textAttributesKey="KOTLIN_INSTANCE_PROPERTY">FIRST</info>
    return <info textAttributesKey="KOTLIN_PACKAGE_FUNCTION_CALL"><info textAttributesKey="KOTLIN_FUNCTION_CALL">testing</info></info>(<info textAttributesKey="KOTLIN_CLASS">Test</info>.<info textAttributesKey="KOTLIN_INSTANCE_PROPERTY">FIRST</info>, <info textAttributesKey="KOTLIN_CLASS">Test</info>.<info textAttributesKey="KOTLIN_INSTANCE_PROPERTY">SECOND</info>)
}