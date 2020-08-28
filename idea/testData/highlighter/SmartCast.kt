// IGNORE_FIR
class <info textAttributesKey="KOTLIN_CLASS">My</info>(val <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY">x</info>: <info textAttributesKey="KOTLIN_CLASS">Int</info>?)

fun <info textAttributesKey="KOTLIN_CLASS">My</info>?.<info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">foo</info>(): <info textAttributesKey="KOTLIN_CLASS">Int</info> {
    if (this == null) return 42
    if (<info textAttributesKey="KOTLIN_INSTANCE_PROPERTY"><info textAttributesKey="KOTLIN_SMART_CAST_RECEIVER">x</info></info> == null) {
        if (<warning textAttributesKey="WARNING_ATTRIBUTES"><info textAttributesKey="KOTLIN_INSTANCE_PROPERTY"><info textAttributesKey="KOTLIN_SMART_CONSTANT"><info textAttributesKey="KOTLIN_SMART_CAST_RECEIVER">x</info></info></info> != null</warning>) {
            <warning textAttributesKey="WARNING_ATTRIBUTES">return</warning> <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY"><info textAttributesKey="KOTLIN_SMART_CAST_RECEIVER"><info textAttributesKey="KOTLIN_SMART_CAST_VALUE">x</info></info></info>
        }
        return 13
    }
    return <info textAttributesKey="KOTLIN_INSTANCE_PROPERTY"><info textAttributesKey="KOTLIN_SMART_CAST_RECEIVER"><info textAttributesKey="KOTLIN_SMART_CAST_VALUE">x</info></info></info>
}
