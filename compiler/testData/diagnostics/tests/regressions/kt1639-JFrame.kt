// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FULL_JDK
// SKIP_TXT

package test

import javax.swing.JFrame

class KFrame() : JFrame() {
    init {
        val x = this.rootPaneCheckingEnabled // make sure field is visible
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, init, javaProperty, localProperty, primaryConstructor, propertyDeclaration,
thisExpression */
