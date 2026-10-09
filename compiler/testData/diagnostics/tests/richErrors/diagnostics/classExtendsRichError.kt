// RUN_PIPELINE_TILL: FRONTEND
class C : <!NON_ERROR_CLASS_EXTENDS_RICH_ERROR!>RichError<!>()
object O : <!NON_ERROR_CLASS_EXTENDS_RICH_ERROR!>RichError<!>()

val o = object : <!NON_ERROR_CLASS_EXTENDS_RICH_ERROR!>RichError<!>() {}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, objectDeclaration, propertyDeclaration */
