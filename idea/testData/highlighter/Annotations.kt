<info descr="null" textAttributesKey="KOTLIN_ANNOTATION">target</info>(<info descr="null" textAttributesKey="KOTLIN_CLASS">AnnotationTarget</info>.<info descr="null" textAttributesKey="KOTLIN_ENUM_ENTRY">CLASSIFIER</info>, <info descr="null" textAttributesKey="KOTLIN_CLASS">AnnotationTarget</info>.<info descr="null" textAttributesKey="KOTLIN_ENUM_ENTRY">EXPRESSION</info>)
<info descr="null">annotation</info> class <info descr="null">Ann</info>

<info descr="null" textAttributesKey="KOTLIN_ANNOTATION">Ann</info> class <info descr="null">A1</info>
<info descr="null" textAttributesKey="KOTLIN_ANNOTATION">@Ann</info> class <info descr="null">A2</info>

fun <info descr="null">bar</info>(<info descr="null">block</info>: () -> <info descr="null">Int</info>) = <info descr="null"><info descr="null">block</info></info>()

<info descr="null" textAttributesKey="KOTLIN_BUILTIN_ANNOTATION">@private</info>
fun <info descr="null">foo</info>() {
    1 + <info descr="null" textAttributesKey="KOTLIN_ANNOTATION">@Ann</info> 2

    <info descr="null" textAttributesKey="KOTLIN_ANNOTATION">@Ann</info> 3 + 4

    <info descr="null"><info descr="null">bar</info></info> <info descr="null" textAttributesKey="KOTLIN_ANNOTATION">@Ann</info> { 1 }

    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: Err" textAttributesKey="WRONG_REFERENCES_ATTRIBUTES">@Err</error>
    <warning descr="[UNUSED_EXPRESSION] The expression is unused" textAttributesKey="WARNING_ATTRIBUTES">5</warning>
}

<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: Err" textAttributesKey="WRONG_REFERENCES_ATTRIBUTES">@Err</error> class <info descr="null" textAttributesKey="KOTLIN_CLASS">Err1</info>

class <info descr="null">NotAnn</info>
<error descr="[NOT_AN_ANNOTATION_CLASS] 'NotAnn' is not an annotation class" textAttributesKey="ERRORS_ATTRIBUTES">@NotAnn</error>
class <info descr="null">C</info>
