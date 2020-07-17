// IGNORE_FIR
// EXPECTED_DUPLICATED_HIGHLIGHTING

val <info textAttributesKey="KOTLIN_PACKAGE_PROPERTY">fnType</info> : <info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info> () -> <info textAttributesKey="KOTLIN_OBJECT">Unit</info> = {}

val <info textAttributesKey="KOTLIN_PACKAGE_PROPERTY">fnFnType</info>: () -> <info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info> () -> <info textAttributesKey="KOTLIN_OBJECT">Unit</info> = {  -> {}}

<info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info> fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">inSuspend</info>(<info textAttributesKey="KOTLIN_PARAMETER">fn</info>: <info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info> () -> <info textAttributesKey="KOTLIN_OBJECT">Unit</info>) {
    val <info textAttributesKey="KOTLIN_LOCAL_VARIABLE">res</info>: <info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info> (<info textAttributesKey="KOTLIN_CLASS">Int</info>) -> <info textAttributesKey="KOTLIN_CLASS">Int</info> = { <info textAttributesKey="KOTLIN_PARAMETER"><info textAttributesKey="KOTLIN_CLOSURE_DEFAULT_PARAMETER">it</info></info> + 1 };
    <info textAttributesKey="KOTLIN_CONSTRUCTOR">T2</info>().<info textAttributesKey="KOTLIN_FUNCTION_CALL">nonSuspend</info>()
    .<info textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL">suspend1</info>(<info textAttributesKey="KOTLIN_PARAMETER">fn</info>)
    .<info textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL">suspend1</info> {  }
        .<info textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL">suspend1</info> { <info textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL"><info textAttributesKey="KOTLIN_LOCAL_VARIABLE">res</info></info>(5) }
    <info textAttributesKey="KOTLIN_LOCAL_VARIABLE"><info textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL">res</info></info>(5)
    <info textAttributesKey="KOTLIN_PACKAGE_PROPERTY"><info textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL">fnType</info></info>()
    <info textAttributesKey="KOTLIN_PACKAGE_PROPERTY"><info textAttributesKey="KOTLIN_VARIABLE_AS_FUNCTION">fnFnType</info></info>().<info textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL">invoke</info>()
}
class <info textAttributesKey="KOTLIN_CLASS">T2</info> {
    <info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info> <info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">inline</info> fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">suspend1</info>(<info textAttributesKey="KOTLIN_PARAMETER">block</info>: <warning textAttributesKey="WARNING_ATTRIBUTES"><info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info></warning> () -> <info textAttributesKey="KOTLIN_OBJECT">Unit</info>): <info textAttributesKey="KOTLIN_CLASS">T2</info> {
        <info textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL"><info textAttributesKey="KOTLIN_PARAMETER">block</info></info>()
        return this
    }
    fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">nonSuspend</info>() = this
}