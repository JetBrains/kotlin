class <info textAttributesKey="KOTLIN_CLASS">My</info>(val <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY"><info textAttributesKey="KOTLIN_PARAMETER"><info textAttributesKey="KOTLIN_PROPERTY_WITH_BACKING_FIELD">x</info></info></info>: <info textAttributesKey="KOTLIN_CLASS">Int</info>?)

fun <info textAttributesKey="KOTLIN_CLASS">My</info>?.<info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">foo</info>(): <info textAttributesKey="KOTLIN_CLASS">Int</info> {
    if (this == null || <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY"><info textAttributesKey="KOTLIN_SMART_CAST_RECEIVER">x</info></info> == null) return 42
    return <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY"><info textAttributesKey="KOTLIN_SMART_CAST_RECEIVER"><info textAttributesKey="KOTLIN_SMART_CAST_VALUE">x</info></info></info>
}
