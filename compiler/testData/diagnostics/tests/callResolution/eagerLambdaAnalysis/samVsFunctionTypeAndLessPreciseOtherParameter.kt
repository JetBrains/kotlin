// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK

interface IBase
interface IDerived : IBase

fun foo1(i: IDerived, runnable: Runnable): Int = 1
fun foo1(i: IBase, runnable: () -> Unit): String = ""

// See com.intellij.util.ui.update.UiNotifyConnector.Companion.doWhenFirstShown
fun foo2(i: IDerived, runnable: Runnable): Int = 1
fun foo2(i: IBase, isDeferred: Boolean = true, runnable: () -> Unit): String = ""

fun myUnit() {}

fun baz(iDerived: IDerived) {
    val x1 = foo1(iDerived) {
        myUnit()
    }

    val x2 = foo2(iDerived) {
        myUnit()
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x2<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, interfaceDeclaration, lambdaLiteral,
localProperty, propertyDeclaration */
