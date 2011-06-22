/*
 * @author max
 */
package org.jetbrains.jet.codegen;

import org.objectweb.asm.commons.Method;

public class GeneratedClosureDescriptor {
    private final String classname;
    private Method constructor;

    public GeneratedClosureDescriptor(String classname, Method constructor) {
        this.classname = classname;
        this.constructor = constructor;
    }

    public String getClassname() {
        return classname;
    }

    public Method getConstructor() {
        return constructor;
    }
}
