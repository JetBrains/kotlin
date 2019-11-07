
<info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info> fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">letsDoSuspendLambda</info>(<info textAttributesKey="KOTLIN_PARAMETER">block</info>: <info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info> () -> <info textAttributesKey="KOTLIN_OBJECT">Unit</info>) {
    val <info textAttributesKey="KOTLIN_LOCAL_VARIABLE">res</info> : <info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info> (<info textAttributesKey="KOTLIN_CLASS">Int</info>) -> <info textAttributesKey="KOTLIN_CLASS">Int</info> = { <info textAttributesKey="KOTLIN_PARAMETER"><info textAttributesKey="KOTLIN_CLOSURE_DEFAULT_PARAMETER" descr=Automatically declared based on the expected type>it</info></info> + 1 };
    <info textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL">letsDoSomething</info> { <warning textAttributesKey="WARNING_ATTRIBUTES" descr=[UNUSED_EXPRESSION] The expression is unused><info textAttributesKey="KOTLIN_WRAPPED_INTO_REF" descr=Value captured in a closure>block</info></warning> }
    <info textAttributesKey="KOTLIN_LOCAL_VARIABLE"><info textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL">res</info></info>(5)
    <info textAttributesKey="KOTLIN_WRAPPED_INTO_REF" descr=Value captured in a closure><info textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL">block</info></info>()
}

<info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info> fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">letsDoSomething</info>(<info textAttributesKey="KOTLIN_PARAMETER">block</info>: <info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info> () -> <info textAttributesKey="KOTLIN_OBJECT">Unit</info>) {
    <info textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL">doSomething</info>()
    <info textAttributesKey="KOTLIN_SUSPEND_FUNCTION_CALL"><info textAttributesKey="KOTLIN_PARAMETER">block</info></info>()
}

<info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">suspend</info> fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">doSomething</info>() {
}