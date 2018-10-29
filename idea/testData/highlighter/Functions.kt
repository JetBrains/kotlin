// WITH_RUNTIME
fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">global</info>() {
    fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">inner</info>() {

    }
    <info textAttributesKey="KOTLIN_FUNCTION_CALL">inner</info>()
}

fun <info textAttributesKey="KOTLIN_CLASS">Int</info>.<info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">ext</info>() {
}

<info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">infix</info> fun <info textAttributesKey="KOTLIN_CLASS">Int</info>.<info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">fif</info>(<info textAttributesKey="KOTLIN_PARAMETER">y</info>: <info textAttributesKey="KOTLIN_CLASS">Int</info>) {
    this * <info textAttributesKey="KOTLIN_PARAMETER">y</info>
}

<info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">open</info> class <info textAttributesKey="KOTLIN_CLASS">Container</info> {
    <info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">open</info> fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">member</info>() {
        <info textAttributesKey="KOTLIN_PACKAGE_FUNCTION_CALL">global</info>()
        5.<info textAttributesKey="KOTLIN_EXTENSION_FUNCTION_CALL">ext</info>()
        <info textAttributesKey="KOTLIN_FUNCTION_CALL">member</info>()
        5 <info textAttributesKey="KOTLIN_EXTENSION_FUNCTION_CALL">fif</info> 6
    }
}

fun <info descr="null" textAttributesKey="KOTLIN_FUNCTION_DECLARATION">foo</info>() {
    <info descr="null" textAttributesKey="KOTLIN_KEYWORD">suspend</info> {

    }
}
