fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">global</info>() {
    fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">inner</info>() {

    }
    <info textAttributesKey="KOTLIN_FUNCTION_CALL">inner</info>()
}

fun Int.<info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">ext</info>() {
}

<info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">open</info> class <info textAttributesKey="KOTLIN_CLASS">Container</info> {
    <info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">open</info> fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">member</info>() {
        <info textAttributesKey="KOTLIN_NAMESPACE_FUNCTION_CALL">global</info>()
        5.<info textAttributesKey="KOTLIN_EXTENSION_FUNCTION_CALL">ext</info>()
        <info textAttributesKey="KOTLIN_FUNCTION_CALL">member</info>()
    }
}
