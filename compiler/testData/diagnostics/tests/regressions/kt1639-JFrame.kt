package test

import javax.swing.JFrame

class KFrame() : JFrame() {
    {
        val x = this.accessibleContext // make sure field is visible
    }
}
