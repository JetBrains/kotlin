// FILE: Frame.java
package awt;

public class Frame {

    String accessibleContext = null;

}

// FILE: JFrame.java
package test;

import awt.Frame;

public class JFrame extends Frame {
    public JFrame() {
    }

    protected String accessibleContext = null;
}

