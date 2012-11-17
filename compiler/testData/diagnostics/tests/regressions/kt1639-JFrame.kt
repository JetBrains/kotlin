package test

import javax.swing.JFrame

class KFrame() : JFrame() {
    {
        val x = this.rootPaneCheckingEnabled // make sure field is visible
    }
}
