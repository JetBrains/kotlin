// FULL_JDK
// SKIP_TXT

package test

import javax.swing.JFrame

class KFrame() : JFrame() {
    init {
        val <!UNUSED_VARIABLE!>x<!> = this.rootPaneCheckingEnabled // make sure field is visible
    }
}
