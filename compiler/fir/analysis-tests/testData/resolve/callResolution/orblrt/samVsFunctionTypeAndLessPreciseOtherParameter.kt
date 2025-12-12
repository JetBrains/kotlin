// RUN_PIPELINE_TILL: FRONTEND
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
    val x1 = <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo1<!>(iDerived) {
        myUnit()
    }

    val x2 = <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo2<!>(iDerived) {
        myUnit()
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Ambiguity: foo1, [/foo1, /foo1]")!>x1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Ambiguity: foo2, [/foo2, /foo2]")!>x2<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, interfaceDeclaration, lambdaLiteral,
localProperty, propertyDeclaration */
