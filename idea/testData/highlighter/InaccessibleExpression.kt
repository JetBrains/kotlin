// IGNORE_FIR

interface <info textAttributesKey="KOTLIN_TRAIT">Zoo</info><<info textAttributesKey="KOTLIN_TYPE_PARAMETER">T</info>> {
    <info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION"><error descr="[WRONG_MODIFIER_TARGET] Modifier 'inner' is not applicable to 'enum class'" textAttributesKey="ERRORS_ATTRIBUTES">inner</error></info> <info textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">enum</info> class <info textAttributesKey="KOTLIN_ENUM">Var</info> : <info textAttributesKey="KOTLIN_TRAIT">Zoo</info><<error descr="[INACCESSIBLE_OUTER_CLASS_EXPRESSION] Expression is inaccessible from a nested class 'Var'" textAttributesKey="ERRORS_ATTRIBUTES">T</error>>
}

object <info textAttributesKey="KOTLIN_OBJECT">Outer</info> {
    fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">bar</info>() = <info textAttributesKey="KOTLIN_OBJECT">Unit</info>
    class <info textAttributesKey="KOTLIN_CLASS">Inner</info>  {
        fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">foo</info>() = this<info textAttributesKey="KOTLIN_LABEL"><error descr="[INACCESSIBLE_OUTER_CLASS_EXPRESSION] Expression is inaccessible from a nested class 'Inner'" textAttributesKey="ERRORS_ATTRIBUTES">@Outer</error></info>.<error descr="[DEBUG] Reference is not resolved to anything, but is not marked unresolved" textAttributesKey="KOTLIN_DEBUG_INFO">bar</error>()
    }
}