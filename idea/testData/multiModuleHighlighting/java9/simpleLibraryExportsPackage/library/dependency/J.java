package dependency;

import dependency.impl.JImpl;

public class J {
    public static JImpl getInstance() { return new JImpl(); }
}
