package org.jetbrains.jet.lang.resolve.calls;

/**
* @author abreslav
*/
public class DefaultValueArgument implements ResolvedValueArgument {
    public static final DefaultValueArgument DEFAULT = new DefaultValueArgument();

    DefaultValueArgument() {}
}
