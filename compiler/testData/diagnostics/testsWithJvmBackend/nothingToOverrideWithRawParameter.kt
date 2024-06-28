// ISSUE: KT-62605
// JVM_TARGET: 1.8

// FILE: Breakpoint.java

public class Breakpoint<P> { }

// FILE: JavaBreakpointType.java

public interface JavaBreakpointType<P> {
    Breakpoint<P> createJavaBreakpoint(Breakpoint<P> breakpoint);
}

// FILE: JavaMethodBreakpointType.java

public class JavaMethodBreakpointType implements JavaBreakpointType<String>{
    @Override
    public Breakpoint<String> createJavaBreakpoint(Breakpoint /* Raw type */ breakpoint) {
        return new Breakpoint<>(); }
}

// FILE: test.kt

class KotlinFunctionBreakpointType : JavaMethodBreakpointType() {
    override <!ACCIDENTAL_OVERRIDE!>fun createJavaBreakpoint(breakpoint: Breakpoint<String>) = Breakpoint<String>()<!>
}
