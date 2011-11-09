/*
 * @author max
 */
package org.jetbrains.jet.codegen;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.List;

public class GeneratedAnonymousClassDescriptor {
    private final String classname;
    private Method constructor;
    private final boolean captureThis;
    private final Type captureReceiver;
    private List<StackValue> args = new ArrayList<StackValue>();

    public GeneratedAnonymousClassDescriptor(String classname, Method constructor, boolean captureThis, Type captureReceiver) {
        this.classname = classname;
        this.constructor = constructor;
        this.captureThis = captureThis;
        this.captureReceiver = captureReceiver;
    }

    public String getClassname() {
        return classname;
    }

    public Method getConstructor() {
        return constructor;
    }

    public void addArg(StackValue local) {
        args.add(local);
    }

    public List<StackValue> getArgs() {
        return args;
    }

    public boolean isCaptureThis() {
        return captureThis;
    }

    public Type isCaptureReceiver() {
        return captureReceiver;
    }
}
