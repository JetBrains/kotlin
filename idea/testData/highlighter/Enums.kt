// IGNORE_FIR

package testing

<info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">enum</info> class <info textAttributesKey="KOTLIN_ENUM">Test</info> {
    <info textAttributesKey="KOTLIN_ENUM_ENTRY">FIRST</info>,
    <info textAttributesKey="KOTLIN_ENUM_ENTRY">SECOND</info>
}

<info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">enum</info> class <info textAttributesKey="KOTLIN_ENUM">Type</info>(val <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY">id</info>: <info textAttributesKey="KOTLIN_CLASS">Int</info>) {
    <info textAttributesKey="KOTLIN_ENUM_ENTRY">FIRST</info>(1),
    <info textAttributesKey="KOTLIN_ENUM_ENTRY">SECOND</info>(2)
}

fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">testing</info>(<info textAttributesKey="KOTLIN_PARAMETER">t1</info>: <info textAttributesKey="KOTLIN_ENUM">Test</info>, <info textAttributesKey="KOTLIN_PARAMETER">t2</info>: <info textAttributesKey="KOTLIN_ENUM">Test</info>): <info textAttributesKey="KOTLIN_ENUM">Test</info> {
    if (<info textAttributesKey="KOTLIN_PARAMETER">t1</info> != <info textAttributesKey="KOTLIN_PARAMETER">t2</info>) return <info textAttributesKey="KOTLIN_ENUM">Test</info>.<info textAttributesKey="KOTLIN_ENUM_ENTRY">FIRST</info>
    return <info textAttributesKey="KOTLIN_PACKAGE_FUNCTION_CALL">testing</info>(<info textAttributesKey="KOTLIN_ENUM">Test</info>.<info textAttributesKey="KOTLIN_ENUM_ENTRY">FIRST</info>, <info textAttributesKey="KOTLIN_ENUM">Test</info>.<info textAttributesKey="KOTLIN_ENUM_ENTRY">SECOND</info>)
}